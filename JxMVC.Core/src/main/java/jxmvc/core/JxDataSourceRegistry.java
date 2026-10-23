package jxmvc.core;

import java.util.concurrent.ConcurrentHashMap;

public final class JxDataSourceRegistry {
    private static final ConcurrentHashMap<String, DSConfig> REGISTRY = new ConcurrentHashMap<>();

    private JxDataSourceRegistry() {}

    public static void register(String name, String url, String user, String pass) {
        REGISTRY.put(name, new DSConfig(url, user, pass));
    }

    public static void registerFromProperties(String name, String prefix) {
        String url  = BaseDbResolver.property(prefix + ".url",  "");
        String user = BaseDbResolver.property(prefix + ".user", "");
        String pass = BaseDbResolver.property(prefix + ".pass", "");
        if (!url.isBlank()) register(name, url, user, pass);
    }

    public static DSConfig get(String name) {
        return REGISTRY.get(name);
    }

    public static boolean has(String name) {
        return REGISTRY.containsKey(name);
    }

    public static void unregister(String name) {
        REGISTRY.remove(name);
    }

    public record DSConfig(String url, String user, String pass) {}
}
