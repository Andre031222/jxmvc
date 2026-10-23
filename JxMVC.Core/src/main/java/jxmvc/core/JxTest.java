package jxmvc.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public final class JxTest {
    private JxTest() {}

    public static Builder invoke(Class<? extends JxController> controllerClass) {
        return new Builder(controllerClass);
    }

    public static void inject(Object target) {
        JxServiceRegistry.inject(target);
    }

    public static void mockService(Class<?> type, Object mock) {
        JxServiceRegistry.register(type, mock);
    }

    public static final class Builder {
        private final Class<? extends JxController> controllerClass;
        private String              httpMethod = "GET";
        private String              body       = "";
        private Map<String, String> pathVars   = new LinkedHashMap<>();
        private Map<String, String> params     = new LinkedHashMap<>();
        private Map<String, String> headers    = new LinkedHashMap<>();
        private String              path       = "/";

        private Builder(Class<? extends JxController> cls) {
            this.controllerClass = cls;
        }

        public Builder method(String method)         { this.httpMethod = method; return this; }
        public Builder path(String path)             { this.path = path; return this; }
        public Builder body(String json)             { this.body = json; return this; }
        public Builder pathVar(String k, String v)   { pathVars.put(k, v); return this; }
        public Builder param(String k, String v)     { params.put(k, v); return this; }
        public Builder header(String k, String v)    { headers.put(k, v); return this; }

        public Response run(String actionName) {
            return run(actionName, new Object[0]);
        }

        public Response run(String actionName, Object... directArgs) {
            try {
                JxController ctrl = controllerClass.getDeclaredConstructor().newInstance();
                JxServiceRegistry.inject(ctrl);

                MockRequest  mockReq  = new MockRequest(httpMethod, path, body, params, headers);
                MockResponse mockResp = new MockResponse();
                JxRequest    jxReq    = mockReq.toJxRequest(mockResp);
                JxResponse   jxResp   = new JxResponse(null, null, "");

                try {
                    java.lang.reflect.Field reqField = JxController.class.getDeclaredField("model");
                    reqField.setAccessible(true);
                    reqField.set(ctrl, jxReq);
                } catch (Exception ignored) {}

                Method method = findMethod(controllerClass, actionName);
                if (method == null) {
                    return new Response(404, "Acción no encontrada: " + actionName, Map.of());
                }
                method.setAccessible(true);

                BaseDispatchPlan plan = new BaseDispatchPlan(
                        actionName, actionName, new String[0],
                        pathVars, controllerClass, method);

                Object result;
                if (directArgs.length > 0) {
                    result = method.invoke(ctrl, directArgs);
                } else if (method.getParameterCount() == 0) {
                    result = method.invoke(ctrl);
                } else {
                    Object[] args = buildTestArgs(plan, method, jxReq);
                    result = method.invoke(ctrl, args);
                }

                if (result instanceof ActionResult ar) {
                    return new Response(ar.hasCustomStatus() ? ar.httpStatus() : 200,
                            ar.payload(), Map.of());
                }
                if (result instanceof String s) {
                    return new Response(200, s, Map.of());
                }
                if (result == null) {
                    return new Response(204, "", Map.of());
                }
                return new Response(200, JxJson.toJson(result), Map.of());
            } catch (java.lang.reflect.InvocationTargetException ex) {
                Throwable cause = ex.getTargetException() != null ? ex.getTargetException() : ex;
                if (cause instanceof JxException jx)
                    return new Response(jx.getStatus(), jx.getMessage(), Map.of());
                return new Response(500, cause.getMessage(), Map.of());
            } catch (Exception ex) {
                return new Response(500, ex.getMessage(), Map.of());
            }
        }

        private static Method findMethod(Class<?> cls, String name) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            return null;
        }

        private Object[] buildTestArgs(BaseDispatchPlan plan, Method method, JxRequest jxReq) {
            Parameter[] params = method.getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                Parameter p = params[i];
                JxMapping.JxPathVar pv = p.getAnnotation(JxMapping.JxPathVar.class);
                JxMapping.JxParam   qp = p.getAnnotation(JxMapping.JxParam.class);
                JxMapping.JxBody    b  = p.getAnnotation(JxMapping.JxBody.class);

                if (pv != null) {
                    String name = pv.value().isBlank() ? p.getName() : pv.value();
                    String val  = pathVars.get(name);
                    args[i] = convertSimple(val, p.getType());
                } else if (qp != null) {
                    String name = qp.value().isBlank() ? p.getName() : qp.value();
                    args[i] = convertSimple(this.params.get(name), p.getType());
                } else if (b != null) {
                    args[i] = JxJson.fromJson(this.body, p.getType());
                    if (p.getAnnotation(JxMapping.JxValid.class) != null && args[i] != null) {
                        JxValidation.validate(args[i]);
                    }
                } else {
                    args[i] = defaultVal(p.getType());
                }
            }
            return args;
        }

        private Object convertSimple(String val, Class<?> type) {
            if (val == null) return defaultVal(type);
            if (type == String.class) return val;
            try {
                if (type == int.class     || type == Integer.class) return Integer.parseInt(val);
                if (type == long.class    || type == Long.class)    return Long.parseLong(val);
                if (type == double.class  || type == Double.class)  return Double.parseDouble(val);
                if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(val);
            } catch (NumberFormatException ignored) {}
            return defaultVal(type);
        }

        private Object defaultVal(Class<?> type) {
            if (type == int.class || type == long.class) return 0;
            if (type == boolean.class) return false;
            if (type == double.class || type == float.class) return 0.0;
            return null;
        }
    }

    public record Response(int status, String body, Map<String, String> headers) {
        public boolean isOk() { return status >= 200 && status < 300; }

        public boolean bodyContains(String fragment) {
            return body != null && body.contains(fragment);
        }

        public <T> T as(Class<T> type) {
            return JxJson.fromJson(body, type);
        }

        @Override
        public String toString() {
            return "JxTest.Response{status=" + status + ", body=" + body + "}";
        }
    }

    private record MockRequest(
            String method, String path, String body,
            Map<String, String> params, Map<String, String> headers) {
        JxRequest toJxRequest(MockResponse resp) {
            return new JxTestRequest(body, params);
        }
    }

    private static final class MockResponse {}

    static final class JxTestRequest extends JxRequest {
        private final String       bodyStr;
        private final Map<String, String> qParams;

        JxTestRequest(String body, Map<String, String> params) {
            super(null, null, new String[0]);
            this.bodyStr = body != null ? body : "";
            this.qParams = params != null ? params : Map.of();
        }

        @Override public String body()           { return bodyStr; }
        @Override public String param(String k)  { return qParams.get(k); }
    }
}
