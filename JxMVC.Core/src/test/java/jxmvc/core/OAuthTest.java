package jxmvc.core;

public class OAuthTest {

    static int passed = 0;
    static int failed = 0;

    static void ok(String label) { passed++; System.out.println("  OK  " + label); }
    static void fail(String label, String msg) { failed++; System.out.println("  FAIL " + label + ": " + msg); }
    static void check(String label, boolean cond) { if (cond) ok(label); else fail(label, "false"); }

    public static void main(String[] args) {
        testPasswordRoundtrip();
        testPasswordFormat();
        testPasswordTampering();
        testPkceChallengeVector();
        testTokenUrlSafe();
        testAuthorizeUrl();
        testUnconfiguredFails();
        System.out.printf("OAuthTest: pass=%d fail=%d%n", passed, failed);
    }

    // ── JxPasswords ────────────────────────────────────────────────────────

    static void testPasswordRoundtrip() {
        String hash = JxPasswords.hash("s3cr3t!");
        check("verify acepta la correcta",      JxPasswords.verify("s3cr3t!", hash));
        check("verify rechaza la incorrecta",   !JxPasswords.verify("otra", hash));
        check("hashes distintos por salt",      !JxPasswords.hash("s3cr3t!").equals(hash));
        check("verify con hash null es false",  !JxPasswords.verify("s3cr3t!", null));
        check("verify con plain null es false", !JxPasswords.verify(null, hash));
    }

    static void testPasswordFormat() {
        String hash = JxPasswords.hash("abc", 1000);
        check("prefijo pbkdf2$sha256$",  hash.startsWith("pbkdf2$sha256$"));
        check("cinco segmentos $",       hash.split("\\$").length == 5);
        check("iteraciones embebidas",   hash.split("\\$")[2].equals("1000"));
        check("verifica coste bajo",     JxPasswords.verify("abc", hash));
        check("needsRehash coste bajo",  JxPasswords.needsRehash(hash));
        check("no rehash coste default", !JxPasswords.needsRehash(JxPasswords.hash("abc")));
    }

    static void testPasswordTampering() {
        check("hash vacío no rompe",      !JxPasswords.verify("x", ""));
        check("hash basura no rompe",     !JxPasswords.verify("x", "pbkdf2$sha256$mal"));
        check("segmentos incompletos",    !JxPasswords.verify("x", "pbkdf2$sha256$1000$YWJj"));
        check("iteraciones no numéricas", !JxPasswords.verify("x", "pbkdf2$sha256$xx$YWJj$YWJj"));
    }

    // ── JxOAuth ────────────────────────────────────────────────────────────

    // Vector de prueba PKCE del RFC 7636, Apéndice B.
    static void testPkceChallengeVector() {
        String verifier  = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String expected  = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        check("code_challenge S256 (RFC 7636)",
                expected.equals(JxOAuth.codeChallengeS256(verifier)));
    }

    static void testTokenUrlSafe() {
        String t = JxOAuth.newToken(24);
        check("token no vacío",     t != null && !t.isBlank());
        check("token URL-safe",     t.matches("[A-Za-z0-9_-]+"));
        check("tokens no repetidos", !JxOAuth.newToken(24).equals(t));
    }

    static void testAuthorizeUrl() {
        JxOAuth.Provider p = new JxOAuth.Provider(
                "https://auth.example/authorize",
                "https://auth.example/token",
                "https://auth.example/userinfo",
                "cid", "sec", "https://app.local/auth/google/callback",
                "openid email profile");
        String url = JxOAuth.of(p).authorizeUrl(p.redirectUri(), "st8", "chal");

        check("base del endpoint",   url.startsWith("https://auth.example/authorize?"));
        check("client_id",           url.contains("client_id=cid"));
        check("response_type=code",  url.contains("response_type=code"));
        check("state",               url.contains("state=st8"));
        check("code_challenge",      url.contains("code_challenge=chal"));
        check("method S256",         url.contains("code_challenge_method=S256"));
        check("redirect_uri codificado",
                url.contains("redirect_uri=https%3A%2F%2Fapp.local%2Fauth%2Fgoogle%2Fcallback"));
        check("scope codificado",    url.contains("scope=openid+email+profile"));
    }

    static void testUnconfiguredFails() {
        JxOAuth.Provider blank = new JxOAuth.Provider(
                "https://a/auth", "https://a/token", "https://a/userinfo",
                "", "", "", "openid email");
        boolean threw = false;
        try {
            JxOAuth.of(blank).authorizeUrl("", "st", "ch");
        } catch (JxException e) {
            threw = e.getStatus() == 500;
        }
        check("authorizeUrl sin credenciales lanza 500", threw);
    }
}
