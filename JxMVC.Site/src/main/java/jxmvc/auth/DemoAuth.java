package jxmvc.auth;

import jxmvc.core.JxOAuth;
import jxmvc.core.JxPasswords;

public final class DemoAuth {
    private static final String DEMO_EMAIL = "demo@jxmvc.dev";
    private static final String DEMO_NAME  = "Usuario Demo";
    private static final String DEMO_HASH  = JxPasswords.hash("jxmvc123");

    private DemoAuth() {}

    public static AppUser authenticate(String email, String password) {
        if (email == null || password == null) return null;
        if (!DEMO_EMAIL.equalsIgnoreCase(email.trim())) return null;
        if (!JxPasswords.verify(password, DEMO_HASH))    return null;
        return new AppUser("demo", DEMO_EMAIL, DEMO_NAME, null, "password", "USER");
    }

    public static AppUser fromGoogle(JxOAuth.User g) {
        String name = (g.name() != null && !g.name().isBlank()) ? g.name() : g.email();
        return new AppUser(g.id(), g.email(), name, g.picture(), "google", "USER");
    }
}
