package jxmvc.core;

public final class JxEnvironment {
    private JxEnvironment() {}

    public static String get(String key) {
        return BaseDbResolver.property(key, null);
    }

    public static String get(String key, String defaultValue) {
        return BaseDbResolver.property(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static long getLong(String key, long defaultValue) {
        String v = get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Long.parseLong(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static double getDouble(String key, double defaultValue) {
        String v = get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key);
        if (v == null || v.isBlank()) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    public static boolean has(String key) {
        return get(key) != null;
    }

    public static boolean is(String key, String expected) {
        String v = get(key);
        return v != null && v.trim().equalsIgnoreCase(expected);
    }

    public static boolean isDev() {
        return is("app.env", "development") || is("app.env", "dev")
            || getBoolean("jxmvc.dev", false);
    }
}
