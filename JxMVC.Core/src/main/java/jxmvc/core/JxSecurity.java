package jxmvc.core;

public final class JxSecurity {
    private static volatile JxAuthProvider provider;

    private JxSecurity() {}

    public static void setProvider(JxAuthProvider p) { provider = p; }
    public static JxAuthProvider getProvider()       { return provider; }
    public static boolean isConfigured()             { return provider != null; }
}
