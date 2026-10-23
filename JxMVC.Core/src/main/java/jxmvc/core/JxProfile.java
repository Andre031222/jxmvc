package jxmvc.core;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class JxProfile {
    private static final Set<String> ACTIVE;

    static {
        String raw = System.getProperty("jxmvc.profiles.active");
        if (raw == null) raw = System.getProperty("jxmvc.profile");
        if (raw == null) raw = System.getenv("JXMVC_PROFILE");
        if (raw == null) raw = BaseDbResolver.property("jxmvc.profiles.active", "");
        if (raw.isBlank()) raw = BaseDbResolver.property("jxmvc.profile", "default");
        ACTIVE = Arrays.stream(raw.split(","))
                       .map(s -> s.trim().toLowerCase())
                       .filter(s -> !s.isBlank())
                       .collect(Collectors.toUnmodifiableSet());
    }

    private JxProfile() {}

    public static String active() {
        return ACTIVE.isEmpty() ? "default" : ACTIVE.iterator().next();
    }

    public static Set<String> activeProfiles() { return ACTIVE; }

    public static boolean is(String profile) {
        return ACTIVE.contains(profile.trim().toLowerCase());
    }

    public static boolean isAny(String... profiles) {
        for (String p : profiles) { if (is(p)) return true; }
        return false;
    }

    public static boolean isAll(String... profiles) {
        for (String p : profiles) { if (!is(p)) return false; }
        return true;
    }

    public static boolean isDev()  { return is("dev")  || is("development") || is("default"); }
    public static boolean isProd() { return is("prod") || is("production"); }
    public static boolean isTest() { return is("test"); }
}
