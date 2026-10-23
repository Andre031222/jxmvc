package jxmvc.core;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JxAdviceRegistry {
    private static final JxLogger     log     = JxLogger.getLogger(JxAdviceRegistry.class);
    private static final List<Object> advices = new CopyOnWriteArrayList<>();
    private static volatile boolean scanned = false;

    private JxAdviceRegistry() {}

    public static void register(Object advice) { advices.add(advice); }

    public static ActionResult handle(Throwable cause) {
        ensureScanned();
        Class<? extends Throwable> exType = cause.getClass();
        for (Object advice : advices) {
            for (Method m : advice.getClass().getDeclaredMethods()) {
                JxMapping.JxExceptionHandler ann = m.getAnnotation(JxMapping.JxExceptionHandler.class);
                if (ann == null) continue;
                for (Class<? extends Throwable> t : ann.value()) {
                    if (!t.isAssignableFrom(exType)) continue;
                    try {
                        m.setAccessible(true);
                        Object result = m.getParameterCount() > 0
                                ? m.invoke(advice, cause)
                                : m.invoke(advice);
                        if (result instanceof ActionResult ar) return ar;
                        if (result != null) return ActionResult.json(result);
                        return ActionResult.json("{}");
                    } catch (Exception e) {
                        log.warn("@JxExceptionHandler {}.{} falló: {}",
                                advice.getClass().getSimpleName(), m.getName(), e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    private static void ensureScanned() {
        if (scanned) return;
        synchronized (JxAdviceRegistry.class) {
            if (scanned) return;
            scanPackage(BaseDbResolver.controllerPackage());
            scanned = true;
        }
    }

    private static void scanPackage(String pkg) {
        String pkgPath = pkg.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = JxAdviceRegistry.class.getClassLoader();
        try {
            Enumeration<URL> resources = cl.getResources(pkgPath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!"file".equalsIgnoreCase(url.getProtocol())) continue;
                File dir = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
                if (dir.listFiles() == null) continue;
                for (File f : dir.listFiles()) {
                    if (!f.getName().endsWith(".class")) continue;
                    String cn = pkg + "." + f.getName().replace(".class", "");
                    try {
                        Class<?> cls = Class.forName(cn);
                        if (cls.isAnnotationPresent(JxMapping.JxControllerAdvice.class)) {
                            advices.add(cls.getDeclaredConstructor().newInstance());
                        }
                    } catch (Exception e) {
                        log.warn("Error cargando @JxControllerAdvice {}: {}", cn, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error escaneando advices en paquete '{}': {}", pkg, e.getMessage());
        }
    }
}
