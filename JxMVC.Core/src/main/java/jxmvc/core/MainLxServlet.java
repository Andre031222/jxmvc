package jxmvc.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@MultipartConfig(maxFileSize = 20_971_520, maxRequestSize = 41_943_040)
public class MainLxServlet extends HttpServlet {
    public static final String VERSION = "3.6.0";

    private static final boolean TRUST_PROXY = BaseDbResolver.propertyBool("jxmvc.trustProxy", false);

    private static final JxLogger log = JxLogger.getLogger(MainLxServlet.class);

    private final BaseDispatcher   dispatcher   = new BaseDispatcher();
    private final BaseCorsResolver corsResolver = new BaseCorsResolver();
    private ExecutorService asyncExecutor;

    @Override
    public void init() throws ServletException {
        java.io.File multipartTmp = null;
        String catalinaBase = System.getProperty("catalina.base");
        String contextName  = getServletContext().getContextPath().replaceAll("^/+", "");
        if (contextName.isEmpty()) contextName = "ROOT";
        if (catalinaBase != null) {
            multipartTmp = new java.io.File(catalinaBase,
                    "work/Catalina/localhost/" + contextName + "/tmp");
        } else {
            java.io.File tomcatHome = new java.io.File(System.getProperty("java.io.tmpdir")).getParentFile();
            multipartTmp = new java.io.File(tomcatHome,
                    "work/Catalina/localhost/" + contextName + "/tmp");
        }
        multipartTmp.mkdirs();

        if (getServletContext().getAttribute("javax.servlet.context.tempdir") == null) {
            java.io.File up = multipartTmp.getParentFile();
            up.mkdirs();
            getServletContext().setAttribute("javax.servlet.context.tempdir", up);
        }

        JxServiceRegistry.values();
        JxScheduler.start();
        JxAdviceRegistry.handle(new RuntimeException("__init__"));

        int asyncSize = BaseDbResolver.propertyInt("jxmvc.async.threads", 8);
        asyncExecutor = buildAsyncExecutor(asyncSize);

        JxScheduler.scheduleAtFixedRate(() -> {
            JxPool pool = JxPool.global();
            if (pool != null) pool.keepAlive();
        }, 180_000, 180_000);

        JxDevMode.init();
        log.info("JxMVC v{} iniciado — perfil activo: {}",
                VERSION, JxProfile.active());
    }

    @Override
    public void destroy() {
        JxScheduler.shutdown();
        JxDevMode.shutdown();
        JxCache.shutdown();
        JxRateLimiter.shutdown();
        JxPool pool = JxPool.global();
        if (pool != null) pool.shutdown();
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try { asyncExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        log.info("JxMVC detenido correctamente");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path  = req.getRequestURI();
        String ctx   = req.getContextPath();
        String local = path.startsWith(ctx) ? path.substring(ctx.length()) : path;

        applySecurityHeaders(resp);

        if (JxGzip.ENABLED) {
            String ae = req.getHeader("Accept-Encoding");
            if (ae != null && ae.contains("gzip")) {
                JxGzip.GzipWrapper gzipResp = new JxGzip.GzipWrapper(resp);
                try {
                    serviceInternal(req, gzipResp, local);
                } finally {
                    if (!resp.isCommitted()) gzipResp.finish(resp, JxGzip.MIN_BYTES);
                }
                return;
            }
        }
        serviceInternal(req, resp, local);
    }

    private void serviceInternal(HttpServletRequest req, HttpServletResponse resp, String local)
            throws ServletException, IOException {
        if ("/jx/health".equals(local)) { handleHealth(resp); return; }
        if ("/jx/info".equals(local) || "/jx/metrics".equals(local) || "/jx/openapi".equals(local)) {
            if (!internalEndpointsExposed()) { sendError(req, resp, 404, "Ruta no encontrada"); return; }
            switch (local) {
                case "/jx/info"    -> handleInfo(resp);
                case "/jx/metrics" -> handleMetrics(resp);
                default            -> handleOpenApi(resp);
            }
            return;
        }

        long startMs    = System.currentTimeMillis();
        int  statusCode = 200;

        BaseDispatchPlan plan  = null;
        JxRequest        model = null;
        JxResponse       view  = null;

        try {
            plan = dispatcher.resolve(req);

            JxMapping.JxRateLimit rl = resolveRateLimit(plan);
            if (rl != null) {
                String clientIp = getClientIp(req);
                String routeKey = req.getMethod().toUpperCase() + ":"
                        + plan.controller() + "#" + plan.action();
                if (!JxRateLimiter.allow(clientIp, routeKey, rl.requests(), rl.window())) {
                    resp.setHeader("Retry-After", String.valueOf(rl.window()));
                    log.warn("Rate limit excedido para IP {} en {}", clientIp, routeKey);
                    sendError(req, resp, 429, "Demasiadas peticiones — intenta en " + rl.window() + "s");
                    statusCode = 429;
                    return;
                }
            }

            if (!checkAuth(req, plan)) {
                sendError(req, resp, 401, "No autorizado");
                statusCode = 401;
                return;
            }

            if (JxCsrf.enabled() && !csrfExempt(plan) && !JxCsrf.validate(req)) {
                sendError(req, resp, 403, "Token CSRF ausente o inválido");
                statusCode = 403;
                return;
            }

            if (!corsResolver.applyAndCheck(req, resp, plan.controllerClass(), plan.actionMethod())) {
                sendError(req, resp, 403, "Acceso denegado por política CORS");
                statusCode = 403;
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            model = new JxRequest(req, resp, plan.args(), plan.pathVars());
            view  = new JxResponse(req, resp, req.getContextPath());

            JxFilterContext filterCtx = new JxFilterContext(model, view,
                    plan.controller(), plan.action());
            if (!JxFilters.runBefore(filterCtx)) return;

            JxController controller = dispatcher.newInstance(plan.controllerClass());
            JxServiceRegistry.inject(controller);
            controller.bindContext(model, view);

            ActionResult beforeResult = runBeforeActions(controller, plan.action(),
                    plan, model, req, resp);
            if (beforeResult != null) {
                writeActionResult(beforeResult, resp, 0, req);
                return;
            }

            populateModelAttrs(controller, plan.action(), req);

            if (plan.actionMethod().isAnnotationPresent(JxMapping.JxAsync.class)) {
                final BaseDispatchPlan  fp     = plan;
                final JxController      fc     = controller;
                final Object[]          fargs  = buildArgs(plan, plan.actionMethod().getParameters(), model, req);
                final String            fmetric = metricKey(local);
                final String            fverb  = req.getMethod();

                asyncExecutor.submit(() -> {
                    try {
                        invokeResolved(fp, fc, fargs);
                    } catch (Exception e) {
                        Throwable cause = e instanceof InvocationTargetException ite
                                && ite.getTargetException() != null
                                ? ite.getTargetException() : e;
                        log.error("@JxAsync falló en {}.{}: {}",
                                fp.controllerClass().getSimpleName(),
                                fp.actionMethod().getName(),
                                cause.getMessage(), cause);
                        JxMetrics.record(fmetric, fverb, 500, 0);
                    }
                });

                resp.setStatus(202);
                write(resp, "application/json;charset=UTF-8",
                        "{\"status\":\"accepted\",\"message\":\"Procesando en segundo plano\"}");
                return;
            }

            Object result = invokeWithRetry(plan, controller, model, req);
            runAfterActions(controller, plan.action());
            JxFilters.runAfter(filterCtx);

            if (!view.hasBodyWritten()) {
                int statusOverride = resolveResponseStatus(plan.actionMethod(),
                        plan.controllerClass());
                render(plan, result, statusOverride, req, resp);
                statusCode = resp.getStatus() > 0 ? resp.getStatus() : 200;
            }
        } catch (JxException ex) {
            statusCode = ex.getStatus();
            tryExceptionHandler(plan, ex, req, resp);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            statusCode = 404;
            sendError(req, resp, 404, "Ruta no encontrada");
        } catch (IllegalArgumentException ex) {
            statusCode = 400;
            log.warn("Petición inválida en {}: {}", local, ex.getMessage());
            sendError(req, resp, 400, safeMessage(ex.getMessage(), "Solicitud inválida"));
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getTargetException() != null ? ex.getTargetException() : ex;
            statusCode = cause instanceof JxException jx ? jx.getStatus() : 500;
            tryExceptionHandler(plan, cause, req, resp);
        } catch (ReflectiveOperationException ex) {
            statusCode = 500;
            log.error("Error de reflexión: {}", ex.getMessage(), ex);
            sendError(req, resp, 500, safeMessage(ex.getMessage(), "Error interno del servidor"));
        } catch (Exception ex) {
            statusCode = 500;
            log.error("Error inesperado: {}", ex.getMessage(), ex);
            sendError(req, resp, 500, safeMessage(ex.getMessage(), "Error interno del servidor"));
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            JxMetrics.record(metricKey(local), req.getMethod(), statusCode, durationMs);
            if (log.isDebugEnabled()) {
                log.debug("{} {} → {} en {}ms", req.getMethod(), local, statusCode, durationMs);
            }
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, Method[]> BEFORE_CACHE    = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, Method[]> AFTER_CACHE     = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, Method[]> MODELATTR_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static Method[] annotatedMethods(java.util.concurrent.ConcurrentHashMap<Class<?>, Method[]> cache,
                                             Class<?> cls, Class<? extends java.lang.annotation.Annotation> ann) {
        return cache.computeIfAbsent(cls, c -> {
            java.util.List<Method> found = new java.util.ArrayList<>();
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(ann)) { m.setAccessible(true); found.add(m); }
            }
            return found.toArray(new Method[0]);
        });
    }

    private ActionResult runBeforeActions(JxController ctrl, String action,
                                          BaseDispatchPlan plan, JxRequest model,
                                          HttpServletRequest req, HttpServletResponse resp) {
        for (Method m : annotatedMethods(BEFORE_CACHE, ctrl.getClass(), JxMapping.JxBeforeAction.class)) {
            JxMapping.JxBeforeAction ann = m.getAnnotation(JxMapping.JxBeforeAction.class);
            if (!appliesTo(ann.only(), ann.except(), action)) continue;
            try {
                Object result = m.getParameterCount() == 0 ? m.invoke(ctrl)
                        : m.invoke(ctrl, buildArgs(plan, m.getParameters(), model, req));
                if (result instanceof ActionResult ar) return ar;
                if (Boolean.FALSE.equals(result))
                    return ActionResult.json("{\"blocked\":true}").status(403);
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ite
                        && ite.getTargetException() != null ? ite.getTargetException() : e;
                log.warn("@JxBeforeAction '{}' falló: {}", m.getName(), cause.getMessage());
                return ActionResult.json("{\"error\":\"interceptor error\"}").status(500);
            }
        }
        return null;
    }

    private void runAfterActions(JxController ctrl, String action) {
        for (Method m : annotatedMethods(AFTER_CACHE, ctrl.getClass(), JxMapping.JxAfterAction.class)) {
            JxMapping.JxAfterAction ann = m.getAnnotation(JxMapping.JxAfterAction.class);
            if (!appliesTo(ann.only(), ann.except(), action)) continue;
            try {
                m.invoke(ctrl);
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ite
                        && ite.getTargetException() != null ? ite.getTargetException() : e;
                log.warn("@JxAfterAction '{}' falló: {}", m.getName(), cause.getMessage());
            }
        }
    }

    private boolean appliesTo(String[] only, String[] except, String action) {
        for (String e : except) { if (e.equalsIgnoreCase(action)) return false; }
        if (only.length == 0) return true;
        for (String o : only)  { if (o.equalsIgnoreCase(action)) return true;  }
        return false;
    }

    private void populateModelAttrs(JxController ctrl, String action, HttpServletRequest req) {
        for (Method m : annotatedMethods(MODELATTR_CACHE, ctrl.getClass(), JxMapping.JxModelAttr.class)) {
            JxMapping.JxModelAttr ann = m.getAnnotation(JxMapping.JxModelAttr.class);
            if (ann.only().length > 0) {
                boolean found = false;
                for (String o : ann.only()) { if (o.equalsIgnoreCase(action)) { found = true; break; } }
                if (!found) continue;
            }
            m.setAccessible(true);
            try {
                Object val = m.invoke(ctrl);
                if (val != null) {
                    String attrName = ann.value().isBlank() ? m.getName() : ann.value();
                    req.setAttribute(attrName, val);
                }
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ite
                        && ite.getTargetException() != null ? ite.getTargetException() : e;
                log.warn("@JxModelAttr '{}' falló: {}", m.getName(), cause.getMessage());
            }
        }
    }

    private boolean csrfExempt(BaseDispatchPlan plan) {
        return plan.actionMethod().isAnnotationPresent(JxMapping.JxCsrfExempt.class)
            || plan.controllerClass().isAnnotationPresent(JxMapping.JxCsrfExempt.class);
    }

    private JxMapping.JxRateLimit resolveRateLimit(BaseDispatchPlan plan) {
        JxMapping.JxRateLimit m = plan.actionMethod().getAnnotation(JxMapping.JxRateLimit.class);
        return m != null ? m : plan.controllerClass().getAnnotation(JxMapping.JxRateLimit.class);
    }

    private String getClientIp(HttpServletRequest req) {
        if (TRUST_PROXY) {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                String candidate = ip.split(",")[0].trim();
                if (!candidate.isEmpty()) return candidate;
            }
            ip = req.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank()) return ip.trim();
        }
        return req.getRemoteAddr();
    }

    private String metricKey(String local) {
        if (local == null || local.isEmpty()) return "/";
        return local.replaceAll("/(\\d+|[0-9a-fA-F-]{16,}|[0-9a-fA-F]{8,})(?=/|$)", "/{n}");
    }

    private boolean checkAuth(HttpServletRequest req, BaseDispatchPlan plan) {
        Method m   = plan.actionMethod();
        Class<?> c = plan.controllerClass();

        boolean requireAuth = m.isAnnotationPresent(JxMapping.JxRequireAuth.class)
                           || c.isAnnotationPresent(JxMapping.JxRequireAuth.class);

        JxMapping.JxRequireRole roleAnn = m.getAnnotation(JxMapping.JxRequireRole.class);
        if (roleAnn == null) roleAnn = c.getAnnotation(JxMapping.JxRequireRole.class);

        JxMapping.JxAuth authAnn = m.getAnnotation(JxMapping.JxAuth.class);
        if (authAnn == null) authAnn = c.getAnnotation(JxMapping.JxAuth.class);

        boolean needed = requireAuth || roleAnn != null || (authAnn != null && authAnn.required());
        if (!needed) return true;
        if (!JxSecurity.isConfigured()) {
            log.error("Ruta protegida en {} pero no hay JxAuthProvider configurado — denegando acceso (fail-closed). "
                    + "Registrar el proveedor con JxSecurity.setProvider(...)",
                    c.getSimpleName());
            return false;
        }

        String[] roles = roleAnn  != null ? roleAnn.value()
                       : authAnn  != null ? authAnn.roles()
                       : new String[0];
        return JxSecurity.getProvider().check(req, roles);
    }

    private int resolveResponseStatus(Method method, Class<?> cls) {
        JxMapping.JxResponseStatus m = method.getAnnotation(JxMapping.JxResponseStatus.class);
        if (m != null) return m.value();
        JxMapping.JxResponseStatus c = cls.getAnnotation(JxMapping.JxResponseStatus.class);
        return c != null ? c.value() : 0;
    }

    private Object invokeWithRetry(BaseDispatchPlan plan, JxController ctrl,
                                   JxRequest model, HttpServletRequest raw)
            throws ReflectiveOperationException {
        JxMapping.JxRetry retry = plan.actionMethod().getAnnotation(JxMapping.JxRetry.class);
        if (retry == null) return invoke(plan, ctrl, model, raw);

        int       maxAttempts = Math.max(1, retry.attempts());
        long      backoff     = retry.backoff();
        Throwable lastError   = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return invoke(plan, ctrl, model, raw);
            } catch (InvocationTargetException ex) {
                lastError = ex.getTargetException() != null ? ex.getTargetException() : ex;
                if (!shouldRetry(retry, lastError) || attempt == maxAttempts) break;
                log.warn("@JxRetry intento {}/{} falló en {}.{}: {}",
                        attempt, maxAttempts,
                        plan.controllerClass().getSimpleName(),
                        plan.actionMethod().getName(),
                        lastError.getMessage());
                if (backoff > 0) {
                    try { Thread.sleep(backoff); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            } catch (ReflectiveOperationException ex) {
                throw ex;
            }
        }
        if (lastError instanceof RuntimeException re) throw re;
        if (lastError instanceof ReflectiveOperationException roe) throw roe;
        throw new JxException(500, lastError != null ? lastError.getMessage() : "Error desconocido");
    }

    @SuppressWarnings("unchecked")
    private boolean shouldRetry(JxMapping.JxRetry retry, Throwable error) {
        Class<? extends Throwable>[] on = retry.on();
        if (on == null || on.length == 0) return true;
        for (Class<? extends Throwable> t : on) {
            if (t.isAssignableFrom(error.getClass())) return true;
        }
        return false;
    }

    private Object invoke(BaseDispatchPlan plan, JxController ctrl,
                          JxRequest model, HttpServletRequest raw)
            throws ReflectiveOperationException {
        Method    method = plan.actionMethod();
        Parameter[] params = method.getParameters();

        JxMapping.JxCacheable cacheable = method.getAnnotation(JxMapping.JxCacheable.class);
        if (cacheable != null && "GET".equalsIgnoreCase(raw.getMethod())) {
            String ckey = cacheable.key().isBlank()
                    ? method.getName() + ":" + raw.getRequestURI()
                            + (raw.getQueryString() != null ? "?" + raw.getQueryString() : "")
                    : cacheable.key();
            Object hit = JxCache.get(cacheable.value()).fetch(ckey);
            if (hit != null) {
                log.debug("@JxCacheable HIT {}:{}", cacheable.value(), ckey);
                return hit;
            }
            Object result = doInvoke(plan, ctrl, params, model, raw);
            if (result != null) JxCache.get(cacheable.value()).put(ckey, result, cacheable.ttl());
            return result;
        }

        Object result = doInvoke(plan, ctrl, params, model, raw);

        JxMapping.JxCacheEvict evict = method.getAnnotation(JxMapping.JxCacheEvict.class);
        if (evict != null) {
            if (evict.key().isBlank()) JxCache.evictAll(evict.value());
            else JxCache.get(evict.value()).evict(evict.key());
            log.debug("@JxCacheEvict {}:{}", evict.value(), evict.key());
        }

        return result;
    }

    private Object doInvoke(BaseDispatchPlan plan, JxController ctrl,
                             Parameter[] params, JxRequest model, HttpServletRequest raw)
            throws ReflectiveOperationException {
        boolean tx = plan.actionMethod().isAnnotationPresent(JxMapping.JxTransactional.class)
                  || plan.controllerClass().isAnnotationPresent(JxMapping.JxTransactional.class);

        if (!tx) {
            return params.length == 0 ? plan.actionMethod().invoke(ctrl)
                    : plan.actionMethod().invoke(ctrl, buildArgs(plan, params, model, raw));
        }

        final Object[]    result = {null};
        final Throwable[] error  = {null};
        JxTransaction.run(() -> {
            try {
                result[0] = params.length == 0
                        ? plan.actionMethod().invoke(ctrl)
                        : plan.actionMethod().invoke(ctrl, buildArgs(plan, params, model, raw));
            } catch (Exception e) { error[0] = e; }
        });
        if (error[0] != null) {
            if (error[0] instanceof InvocationTargetException ite) throw ite;
            if (error[0] instanceof ReflectiveOperationException roe) throw roe;
            throw new JxException(500, error[0].getMessage());
        }
        return result[0];
    }

    private void invokeResolved(BaseDispatchPlan plan, JxController ctrl, Object[] args)
            throws ReflectiveOperationException {
        boolean tx = plan.actionMethod().isAnnotationPresent(JxMapping.JxTransactional.class)
                  || plan.controllerClass().isAnnotationPresent(JxMapping.JxTransactional.class);
        if (!tx) { plan.actionMethod().invoke(ctrl, args); return; }

        final Throwable[] error = {null};
        JxTransaction.run(() -> {
            try { plan.actionMethod().invoke(ctrl, args); }
            catch (Exception e) { error[0] = e; }
        });
        if (error[0] instanceof ReflectiveOperationException roe) throw roe;
        if (error[0] != null) throw new JxException(500, error[0].getMessage());
    }

    private Object[] buildArgs(BaseDispatchPlan plan, Parameter[] params,
                                JxRequest model, HttpServletRequest raw) {
        Object[] args = new Object[params.length];
        int posIdx = 0;
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];

            JxMapping.JxPathVar     pathVar = p.getAnnotation(JxMapping.JxPathVar.class);
            JxMapping.JxParam       qParam  = p.getAnnotation(JxMapping.JxParam.class);
            JxMapping.JxBody        body    = p.getAnnotation(JxMapping.JxBody.class);
            JxMapping.JxRequestHeader hdr   = p.getAnnotation(JxMapping.JxRequestHeader.class);
            JxMapping.JxCookieValue cookie  = p.getAnnotation(JxMapping.JxCookieValue.class);

            if (pathVar != null) {
                String name = (pathVar.value().isBlank() ? p.getName() : pathVar.value()).toLowerCase();
                String val  = plan.pathVars().get(name);
                if (val == null && posIdx < plan.args().length) val = plan.args()[posIdx++];
                args[i] = val != null ? convert(val, p.getType(), i) : defaultValue(p.getType());
            } else if (qParam != null) {
                String name = qParam.value().isBlank() ? p.getName() : qParam.value();
                String val  = model.param(name);
                if (val == null && qParam.required())
                    throw new JxException(400, "Parámetro requerido: " + name);
                args[i] = val != null ? convert(val, p.getType(), i) : defaultValue(p.getType());
            } else if (body != null) {
                String ct = raw.getContentType();
                boolean json = ct != null
                        && (ct.toLowerCase().startsWith("application/json") || ct.toLowerCase().contains("+json"));
                if (!json)
                    throw new JxException(415, "Content-Type debe ser application/json");
                Object parsed = JxJson.fromJson(model.body(), p.getType());
                if (p.getAnnotation(JxMapping.JxValid.class) != null && parsed != null) {
                    JxValidation.validate(parsed);
                }
                args[i] = parsed;
            } else if (hdr != null) {
                String val = raw.getHeader(hdr.value());
                if (val == null) val = hdr.defaultValue();
                if (val.isEmpty() && hdr.required())
                    throw new JxException(400, "Cabecera requerida: " + hdr.value());
                args[i] = convert(val, p.getType(), i);
            } else if (cookie != null) {
                String val = findCookie(raw, cookie.value());
                if (val == null) val = cookie.defaultValue();
                if (val.isEmpty() && cookie.required())
                    throw new JxException(400, "Cookie requerida: " + cookie.value());
                args[i] = convert(val, p.getType(), i);
            } else {
                String val = posIdx < plan.args().length ? plan.args()[posIdx++] : "";
                args[i] = convert(val, p.getType(), i);
            }
        }
        return args;
    }

    private String findCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) { if (name.equals(c.getName())) return c.getValue(); }
        return null;
    }

    private Object convert(String raw, Class<?> type, int i) {
        if (raw == null || raw.isEmpty()) return defaultValue(type);
        String v = BaseSanitizer.clean(raw);
        if (type == String.class) return v;
        try {
            if (type == int.class     || type == Integer.class) return Integer.parseInt(v);
            if (type == long.class    || type == Long.class)    return Long.parseLong(v);
            if (type == double.class  || type == Double.class)  return Double.parseDouble(v);
            if (type == float.class   || type == Float.class)   return Float.parseFloat(v);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(v);
        } catch (NumberFormatException ex) {
            throw new JxException(400, "Argumento " + i + " inválido para " + type.getSimpleName());
        }
        log.error("Tipo de parámetro no soportado en la acción: {}", type.getName());
        throw new JxException(400, "Argumento " + i + " de tipo no soportado");
    }

    private Object defaultValue(Class<?> type) {
        if (type == int.class || type == long.class || type == short.class || type == byte.class) return 0;
        if (type == double.class || type == float.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return null;
    }

    private void tryExceptionHandler(BaseDispatchPlan plan, Throwable cause,
                                     HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (plan != null) {
            try {
                Method handler = findControllerHandler(plan.controllerClass(), cause.getClass());
                if (handler != null) {
                    JxController ctrl = dispatcher.newInstance(plan.controllerClass());
                    JxRequest  model  = new JxRequest(req, resp, new String[0]);
                    JxResponse view   = new JxResponse(req, resp, req.getContextPath());
                    ctrl.bindContext(model, view);
                    JxServiceRegistry.inject(ctrl);
                    Object result = handler.invoke(ctrl, cause);
                    if (!view.hasBodyWritten()) render(plan, result, 0, req, resp);
                    return;
                }
            } catch (Exception e) {
                log.warn("@JxExceptionHandler en {} falló: {}",
                        plan.controllerClass().getSimpleName(), e.getMessage());
            }
        }

        ActionResult advice = JxAdviceRegistry.handle(cause);
        if (advice != null) { writeActionResult(advice, resp); return; }

        if (cause instanceof JxException jx) {
            sendError(req, resp, jx.getStatus(), jx.getMessage());
        } else {
            log.error("Error sin handler: {}", cause.getMessage(), cause);
            sendError(req, resp, 500, safeMessage(cause.getMessage(), "Error interno del servidor"));
        }
    }

    private Method findControllerHandler(Class<? extends JxController> cls,
                                         Class<? extends Throwable> exType) {
        for (Method m : cls.getDeclaredMethods()) {
            JxMapping.JxExceptionHandler ann = m.getAnnotation(JxMapping.JxExceptionHandler.class);
            if (ann == null) continue;
            for (Class<? extends Throwable> t : ann.value()) {
                if (t.isAssignableFrom(exType)) return m;
            }
        }
        return null;
    }

    private void render(BaseDispatchPlan plan, Object result, int statusOverride,
                        HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (result == null) {
            if (statusOverride > 0) resp.setStatus(statusOverride);
            forwardView(plan.controller() + "/" + plan.action(), req, resp);
            return;
        }

        if (result instanceof ActionResult ar) {
            writeActionResult(ar, resp, statusOverride, req);
            return;
        }

        if (result instanceof String s) {
            if (statusOverride > 0) resp.setStatus(statusOverride);
            if (s.startsWith("redirect:"))  { resp.sendRedirect(JxResponse.checkRedirect(s.substring(9))); return; }
            if (s.endsWith(".jsp")) {
                if (!safeViewPath(s)) { sendError(req, resp, 404, "Vista no encontrada"); return; }
                req.getRequestDispatcher(s).forward(req, resp);
                return;
            }
            write(resp, "text/plain;charset=UTF-8", s);
            return;
        }

        String accept = req.getHeader("Accept");
        if (statusOverride > 0) resp.setStatus(statusOverride);

        if (accept != null && accept.contains("text/html")
                && !accept.contains("application/json")) {
            String viewPath = plan.controller() + "/" + plan.action();
            if (safeViewPath(viewPath)) {
                try {
                    req.setAttribute("model", result);
                    req.getRequestDispatcher("/WEB-INF/views/" + viewPath + ".jsp")
                       .forward(req, resp);
                    return;
                } catch (Exception e) {
                    if (resp.isCommitted()) {
                        log.error("Vista {} falló con respuesta ya comprometida: {}", viewPath, e.getMessage());
                        return;
                    }
                }
            }
            write(resp, "application/json;charset=UTF-8", JxJson.toJson(result));
            return;
        }

        write(resp, "application/json;charset=UTF-8", JxJson.toJson(result));
    }

    private void writeActionResult(ActionResult ar, HttpServletResponse resp)
            throws ServletException, IOException {
        writeActionResult(ar, resp, 0, null);
    }

    private void writeActionResult(ActionResult ar, HttpServletResponse resp,
                                   int statusOverride, HttpServletRequest req)
            throws ServletException, IOException {
        ar.headers().forEach(resp::setHeader);
        int code = ar.hasCustomStatus() ? ar.httpStatus()
                 : statusOverride > 0   ? statusOverride : 0;
        if (code > 0) resp.setStatus(code);

        switch (ar.type()) {
            case VIEW     -> { if (req != null) forwardView(ar.payload(), req, resp); }
            case TEXT     -> write(resp, "text/plain;charset=UTF-8",       ar.payload());
            case JSON     -> write(resp, "application/json;charset=UTF-8", ar.payload());
            case REDIRECT -> {
                if (req != null)
                    resp.sendRedirect(req.getContextPath()
                            + (ar.payload().startsWith("/") ? "" : "/") + ar.payload());
                else
                    resp.sendRedirect(JxResponse.checkRedirect(ar.payload()));
            }
        }
    }

    private void forwardView(String path, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!safeViewPath(path)) { sendError(req, resp, 404, "Vista no encontrada"); return; }
        req.getRequestDispatcher("/WEB-INF/views/" + path + ".jsp").forward(req, resp);
    }

    private void write(HttpServletResponse resp, String contentType, String body) throws IOException {
        if (resp.getContentType() == null) resp.setContentType(contentType);
        resp.getWriter().write(body);
    }

    private void handleHealth(HttpServletResponse resp) throws IOException {
        boolean schedOk = JxScheduler.isRunning();
        JxPool  pool    = JxPool.global();
        Object poolJson = pool != null
                ? GenApi.nested("enabled", true, "available", pool.available(), "total", pool.total())
                : GenApi.nested("enabled", false);
        String body = hideInternals()
            ? GenApi.JsonStr(
                "status",    "UP",
                "pool",      poolJson,
                "scheduler", GenApi.nested("running", schedOk),
                "async",     GenApi.nested("active", asyncExecutor != null && !asyncExecutor.isShutdown()),
                "ws",        GenApi.nested("connections", JxWebSocket.totalConnections()))
            : GenApi.JsonStr(
                "status",    "UP",
                "version",   VERSION,
                "profile",   JxProfile.active(),
                "devMode",   JxDevMode.isActive(),
                "pool",      poolJson,
                "scheduler", GenApi.nested("running", schedOk),
                "async",     GenApi.nested("active", asyncExecutor != null && !asyncExecutor.isShutdown()),
                "ws",        GenApi.nested("connections", JxWebSocket.totalConnections()));
        resp.setStatus(200);
        write(resp, "application/json;charset=UTF-8", body);
    }

    private void handleInfo(HttpServletResponse resp) throws IOException {
        String body = GenApi.JsonStr(
            "framework", "JxMVC",
            "version",   VERSION,
            "profile",   JxProfile.active(),
            "devMode",   JxDevMode.isActive(),
            "java",      System.getProperty("java.version"),
            "server",    getServletContext().getServerInfo()
        );
        resp.setStatus(200);
        write(resp, "application/json;charset=UTF-8", body);
    }

    private void handleMetrics(HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        write(resp, "application/json;charset=UTF-8", JxMetrics.toJson());
    }

    private void handleOpenApi(HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        write(resp, "application/json;charset=UTF-8", JxOpenApi.generate());
    }

    private void applySecurityHeaders(HttpServletResponse resp) {
        resp.setHeader("X-Content-Type-Options", "nosniff");

        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        String frameOpt = BaseDbResolver.property("jxmvc.security.frame-options", "SAMEORIGIN");
        if (!"false".equalsIgnoreCase(frameOpt)) {
            resp.setHeader("X-Frame-Options", frameOpt.toUpperCase());
        }

        if ("true".equalsIgnoreCase(BaseDbResolver.property("jxmvc.security.hsts", "false"))) {
            String maxAge = BaseDbResolver.property("jxmvc.security.hsts.maxage", "31536000");
            resp.setHeader("Strict-Transport-Security",
                    "max-age=" + maxAge + "; includeSubDomains");
        }

        String csp = BaseDbResolver.property("jxmvc.security.csp", "");
        if (csp.isBlank()) csp = defaultCsp(frameOpt);
        if (!"false".equalsIgnoreCase(csp)) resp.setHeader("Content-Security-Policy", csp);
    }

    static String defaultCsp(String frameOptions) {
        String base = "base-uri 'self'; object-src 'none'";
        if ("DENY".equalsIgnoreCase(frameOptions)) return base + "; frame-ancestors 'none'";
        if ("false".equalsIgnoreCase(frameOptions)) return base;
        return base + "; frame-ancestors 'self'";
    }

    private static ExecutorService buildAsyncExecutor(int platformPoolSize) {
        try {
            java.lang.reflect.Method m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            ExecutorService vt = (ExecutorService) m.invoke(null);
            log.info("@JxAsync usando Virtual Threads (Java 21+)");
            return vt;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.warn("Virtual Threads no disponibles: {}", e.getMessage());
        }
        log.info("@JxAsync usando pool de {} hilos de plataforma", platformPoolSize);
        return Executors.newFixedThreadPool(platformPoolSize, r -> {
            Thread t = new Thread(r, "jx-async-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    private void sendError(HttpServletRequest req, HttpServletResponse resp, int code, String message)
            throws ServletException, IOException {
        if (resp.isCommitted()) return;
        String msg = message == null ? "Error" : message;
        if (Boolean.TRUE.equals(req.getAttribute("jx_in_error"))) {
            resp.setStatus(code);
            writeError(req, resp, code, msg);
            return;
        }
        req.setAttribute("jx_in_error",      Boolean.TRUE);
        req.setAttribute("jx_error_code",    code);
        req.setAttribute("jx_error_message", msg);
        resp.setStatus(code);
        if (wantsJson(req) || !hasErrorView(req)) {
            writeError(req, resp, code, msg);
            return;
        }
        try {
            req.getRequestDispatcher(ERROR_VIEW).forward(req, resp);
        } catch (Exception ex) {
            if (!resp.isCommitted()) writeError(req, resp, code, msg);
        }
    }

    private static final String ERROR_VIEW = "/WEB-INF/views/shared/error.jsp";

    private boolean hasErrorView(HttpServletRequest req) {
        try {
            return req.getServletContext().getResource(ERROR_VIEW) != null;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    private boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String ct     = req.getContentType();
        return (accept != null && accept.contains("application/json"))
            || (ct     != null && ct.contains("application/json"));
    }

    private void writeError(HttpServletRequest req, HttpServletResponse resp, int code, String msg)
            throws IOException {
        if (wantsJson(req)) {
            write(resp, "application/json;charset=UTF-8", jsonError(code, msg));
        } else {
            write(resp, "text/plain;charset=UTF-8", code + " " + msg);
        }
    }

    static String jsonError(int code, String msg) {
        return "{\"error\":" + code + ",\"message\":" + JxJson.quote(msg == null ? "Error" : msg) + "}";
    }

    static String safeMessage(String raw, String fallback) {
        return safeMessage(raw, fallback, hideInternals());
    }

    static String safeMessage(String raw, String fallback, boolean hide) {
        if (raw == null || raw.isBlank()) return fallback;
        return hide ? fallback : raw;
    }

    static boolean safeViewPath(String path) {
        return path != null && !path.contains("..") && !path.contains("\\") && !path.contains("//");
    }

    private static boolean hideInternals() {
        return JxProfile.isProd() && !JxDevMode.isActive();
    }

    private static boolean internalEndpointsExposed() {
        String prop = BaseDbResolver.property("jxmvc.internal.expose", "");
        if (!prop.isBlank()) return "true".equalsIgnoreCase(prop);
        return !hideInternals();
    }
}
