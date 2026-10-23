package jxmvc.core;

import java.lang.reflect.Field;

public final class JxConfigBinder {
    private JxConfigBinder() {}

    public static <T> T bind(Class<T> cls) {
        JxMapping.JxConfigProperties ann = cls.getAnnotation(JxMapping.JxConfigProperties.class);
        if (ann == null)
            throw new IllegalArgumentException(cls.getName() + " no tiene @JxConfigProperties");
        return bind(cls, ann.value());
    }

    public static <T> T bind(Class<T> cls, String prefix) {
        T instance;
        try {
            instance = cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new JxException(500, "No se puede instanciar " + cls.getName() + ": " + e.getMessage());
        }

        String pfx = (prefix == null || prefix.isBlank()) ? "" : prefix.trim() + ".";

        for (Field f : collectFields(cls)) {
            f.setAccessible(true);
            String key = pfx + f.getName();
            String keyKebab = pfx + toKebab(f.getName());

            String raw = BaseDbResolver.property(key, null);
            if (raw == null) raw = BaseDbResolver.property(keyKebab, null);
            if (raw == null) continue;

            try {
                f.set(instance, coerce(raw.trim(), f.getType()));
            } catch (IllegalAccessException e) {
                JxLogger.getLogger(JxConfigBinder.class)
                        .warn("@JxConfigProperties falló en {}.{}: {}", cls.getSimpleName(), f.getName(), e.getMessage());
            }
        }

        return instance;
    }

    private static Object coerce(String val, Class<?> type) {
        if (type == String.class)                              return val;
        if (type == int.class     || type == Integer.class)    { try { return Integer.parseInt(val); }   catch (Exception e) { return 0; } }
        if (type == long.class    || type == Long.class)       { try { return Long.parseLong(val); }     catch (Exception e) { return 0L; } }
        if (type == double.class  || type == Double.class)     { try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; } }
        if (type == float.class   || type == Float.class)      { try { return Float.parseFloat(val); }   catch (Exception e) { return 0f; } }
        if (type == boolean.class || type == Boolean.class)    return Boolean.parseBoolean(val);
        return val;
    }

    private static java.util.List<Field> collectFields(Class<?> cls) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) fields.add(f);
            cls = cls.getSuperclass();
        }
        return fields;
    }

    private static String toKebab(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) { sb.append('-'); sb.append(Character.toLowerCase(c)); }
            else sb.append(c);
        }
        return sb.toString();
    }
}
