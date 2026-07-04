/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inicio de sesión con proveedores OAuth 2.0 / OpenID Connect — cero dependencias
 * externas (usa {@link JxHttp} para las llamadas de servidor y {@link JxJson} para el
 * parseo). Implementa el flujo <i>Authorization Code</i> con PKCE (S256).
 *
 * <p>Trae preconfigurado <b>Google</b>. Las credenciales se leen de
 * {@code application.properties} o de variables de entorno equivalentes:
 * <pre>
 *   jxmvc.oauth.google.client-id     = ...    (env JXMVC_OAUTH_GOOGLE_CLIENT_ID)
 *   jxmvc.oauth.google.client-secret = ...    (env JXMVC_OAUTH_GOOGLE_CLIENT_SECRET)
 *   jxmvc.oauth.google.redirect-uri  = https://mi-app/auth/google/callback
 *   jxmvc.oauth.google.scopes        = openid email profile   (opcional)
 * </pre>
 *
 * <p>Uso típico en un controlador:
 * <pre>
 *   // 1) Redirigir al consentimiento de Google
 *   JxOAuth.Flow flow = JxOAuth.google().start();
 *   sessionSet("oauth.state",    flow.state());
 *   sessionSet("oauth.verifier", flow.codeVerifier());
 *   return redirect(flow.url());
 *
 *   // 2) En el callback (/auth/google/callback?code=...&amp;state=...)
 *   if (!flow.state().equals(sessionGet("oauth.state"))) throw JxException.forbidden("state inválido");
 *   JxOAuth.User user = JxOAuth.google().login(param("code"), (String) sessionGet("oauth.verifier"));
 *   sessionSet("user", user);
 * </pre>
 */
public final class JxOAuth {

    private static final JxLogger log = JxLogger.getLogger(JxOAuth.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Endpoints y credenciales de un proveedor OAuth 2.0 / OIDC. */
    public record Provider(
            String authUrl,
            String tokenUrl,
            String userInfoUrl,
            String clientId,
            String clientSecret,
            String redirectUri,
            String scopes) {
    }

    /** Datos para arrancar el flujo: URL de consentimiento + secretos a guardar en sesión. */
    public record Flow(String url, String state, String codeVerifier) {}

    /** Respuesta del token endpoint. */
    public record Tokens(String accessToken, String idToken, String refreshToken,
                         String tokenType, long expiresIn) {}

    /** Identidad del usuario autenticado (claims OIDC estándar). */
    public record User(String id, String email, boolean emailVerified, String name,
                       String givenName, String familyName, String picture, String locale) {}

    private final Provider provider;

    private JxOAuth(Provider provider) { this.provider = provider; }

    // ── Fábricas ──────────────────────────────────────────────────────────

    /** Instancia sobre un proveedor arbitrario (GitHub, Microsoft, Keycloak, …). */
    public static JxOAuth of(Provider provider) {
        if (provider == null) throw new IllegalArgumentException("provider no puede ser null");
        return new JxOAuth(provider);
    }

    /** Proveedor Google construido desde la configuración de la aplicación. */
    public static JxOAuth google() {
        return of(googleProvider());
    }

    /** {@code true} si Google tiene client-id y client-secret configurados. */
    public static boolean isGoogleConfigured() {
        return notBlank(JxEnvironment.get("jxmvc.oauth.google.client-id"))
            && notBlank(JxEnvironment.get("jxmvc.oauth.google.client-secret"));
    }

    private static Provider googleProvider() {
        return new Provider(
                "https://accounts.google.com/o/oauth2/v2/auth",
                "https://oauth2.googleapis.com/token",
                "https://openidconnect.googleapis.com/v1/userinfo",
                JxEnvironment.get("jxmvc.oauth.google.client-id",     ""),
                JxEnvironment.get("jxmvc.oauth.google.client-secret", ""),
                JxEnvironment.get("jxmvc.oauth.google.redirect-uri",  ""),
                JxEnvironment.get("jxmvc.oauth.google.scopes", "openid email profile"));
    }

    // ── Paso 1: URL de autorización ───────────────────────────────────────

    /**
     * Arranca el flujo: genera {@code state} anti-CSRF y verificador PKCE, y construye
     * la URL de consentimiento. Guardar {@code state} y {@code codeVerifier} en la
     * sesión para validarlos en el callback.
     */
    public Flow start() {
        return start(provider.redirectUri());
    }

    /** Igual que {@link #start()} pero con un {@code redirectUri} explícito. */
    public Flow start(String redirectUri) {
        String state        = newToken(24);
        String codeVerifier = newToken(48);
        String url          = authorizeUrl(redirectUri, state, codeChallengeS256(codeVerifier));
        return new Flow(url, state, codeVerifier);
    }

    /** Construye la URL de autorización de bajo nivel (sin gestionar la sesión). */
    public String authorizeUrl(String redirectUri, String state, String codeChallenge) {
        requireConfigured(redirectUri);
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id",             provider.clientId());
        q.put("redirect_uri",          redirectUri);
        q.put("response_type",         "code");
        q.put("scope",                 provider.scopes());
        q.put("state",                 state);
        q.put("access_type",           "offline");
        q.put("include_granted_scopes","true");
        q.put("prompt",                "select_account");
        if (codeChallenge != null && !codeChallenge.isBlank()) {
            q.put("code_challenge",        codeChallenge);
            q.put("code_challenge_method", "S256");
        }
        return provider.authUrl() + "?" + formEncode(q);
    }

    // ── Paso 2: intercambio de código y datos de usuario ──────────────────

    /** Flujo completo del callback: intercambia el código y devuelve la identidad. */
    public User login(String code, String codeVerifier) {
        return login(code, codeVerifier, provider.redirectUri());
    }

    /** Igual que {@link #login(String, String)} con {@code redirectUri} explícito. */
    public User login(String code, String codeVerifier, String redirectUri) {
        Tokens tokens = exchange(code, codeVerifier, redirectUri);
        return userInfo(tokens.accessToken());
    }

    /** Intercambia el {@code authorization_code} por tokens en el token endpoint. */
    public Tokens exchange(String code, String codeVerifier, String redirectUri) {
        requireConfigured(redirectUri);
        if (code == null || code.isBlank())
            throw JxException.badRequest("Falta el authorization code de OAuth");

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type",    "authorization_code");
        form.put("code",          code);
        form.put("client_id",     provider.clientId());
        form.put("client_secret", provider.clientSecret());
        form.put("redirect_uri",  redirectUri);
        if (codeVerifier != null && !codeVerifier.isBlank())
            form.put("code_verifier", codeVerifier);

        JxHttp.Response resp = JxHttp.builder("POST", provider.tokenUrl())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept",       "application/json")
                .body(formEncode(form))
                .send();

        if (!resp.isOk()) {
            log.warn("OAuth token endpoint respondió {}: {}", resp.status(), resp.body());
            throw new JxException(502, "El proveedor OAuth rechazó el intercambio de código");
        }

        Map<String, Object> json = asMap(resp.body());
        return new Tokens(
                str(json.get("access_token")),
                str(json.get("id_token")),
                str(json.get("refresh_token")),
                str(json.get("token_type")),
                asLong(json.get("expires_in")));
    }

    /** Recupera los claims del usuario desde el userinfo endpoint con el access token. */
    public User userInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank())
            throw new JxException(502, "El proveedor OAuth no devolvió access token");

        JxHttp.Response resp = JxHttp.get(provider.userInfoUrl(),
                Map.of("Authorization", "Bearer " + accessToken, "Accept", "application/json"));

        if (!resp.isOk()) {
            log.warn("OAuth userinfo respondió {}: {}", resp.status(), resp.body());
            throw new JxException(502, "No se pudo obtener el perfil del proveedor OAuth");
        }

        Map<String, Object> json = asMap(resp.body());
        return new User(
                str(json.get("sub")),
                str(json.get("email")),
                asBool(json.get("email_verified")),
                str(json.get("name")),
                str(json.get("given_name")),
                str(json.get("family_name")),
                str(json.get("picture")),
                str(json.get("locale")));
    }

    // ── Utilidades PKCE / state ───────────────────────────────────────────

    /** Token aleatorio URL-safe de {@code bytes} bytes de entropía. */
    public static String newToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** {@code code_challenge} PKCE = BASE64URL(SHA-256(code_verifier)). */
    public static String codeChallengeS256(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private void requireConfigured(String redirectUri) {
        if (!notBlank(provider.clientId()) || !notBlank(provider.clientSecret()))
            throw new JxException(500, "OAuth sin configurar: falta client-id o client-secret");
        if (!notBlank(redirectUri))
            throw new JxException(500, "OAuth sin configurar: falta redirect-uri");
    }

    private static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(String body) {
        Object parsed = JxJson.fromJson(body, Map.class);
        if (parsed instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new JxException(502, "Respuesta OAuth no es un objeto JSON válido");
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return o == null ? 0L : Long.parseLong(String.valueOf(o).trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean b) return b;
        return o != null && Boolean.parseBoolean(String.valueOf(o).trim());
    }
}
