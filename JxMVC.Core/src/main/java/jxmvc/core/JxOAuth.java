package jxmvc.core;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JxOAuth {
    private static final JxLogger log = JxLogger.getLogger(JxOAuth.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    public record Provider(
            String authUrl,
            String tokenUrl,
            String userInfoUrl,
            String clientId,
            String clientSecret,
            String redirectUri,
            String scopes,
            String issuer,
            String jwksUrl) {
        public Provider(String authUrl, String tokenUrl, String userInfoUrl, String clientId,
                        String clientSecret, String redirectUri, String scopes) {
            this(authUrl, tokenUrl, userInfoUrl, clientId, clientSecret, redirectUri, scopes, "", "");
        }
    }

    public record Flow(String url, String state, String codeVerifier) {}

    public record Tokens(String accessToken, String idToken, String refreshToken,
                         String tokenType, long expiresIn) {}

    public record User(String id, String email, boolean emailVerified, String name,
                       String givenName, String familyName, String picture, String locale) {}

    private final Provider provider;

    private JxOAuth(Provider provider) { this.provider = provider; }

    public static JxOAuth of(Provider provider) {
        if (provider == null) throw new IllegalArgumentException("provider no puede ser null");
        return new JxOAuth(provider);
    }

    public static JxOAuth google() {
        return of(googleProvider());
    }

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
                JxEnvironment.get("jxmvc.oauth.google.scopes", "openid email profile"),
                "https://accounts.google.com",
                "https://www.googleapis.com/oauth2/v3/certs");
    }

    public Flow start() {
        return start(provider.redirectUri());
    }

    public Flow start(String redirectUri) {
        String state        = newToken(24);
        String codeVerifier = newToken(48);
        String url          = authorizeUrl(redirectUri, state, codeChallengeS256(codeVerifier));
        return new Flow(url, state, codeVerifier);
    }

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

    public User login(String code, String codeVerifier) {
        return login(code, codeVerifier, provider.redirectUri());
    }

    public User login(String code, String codeVerifier, String redirectUri) {
        Tokens tokens = exchange(code, codeVerifier, redirectUri);
        if (notBlank(provider.jwksUrl()) && notBlank(tokens.idToken()))
            return verifyIdToken(tokens.idToken());
        return userInfo(tokens.accessToken());
    }

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

    public User userInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank())
            throw new JxException(502, "El proveedor OAuth no devolvió access token");

        JxHttp.Response resp = JxHttp.get(provider.userInfoUrl(),
                Map.of("Authorization", "Bearer " + accessToken, "Accept", "application/json"));

        if (!resp.isOk()) {
            log.warn("OAuth userinfo respondió {}: {}", resp.status(), resp.body());
            throw new JxException(502, "No se pudo obtener el perfil del proveedor OAuth");
        }

        return userFromClaims(asMap(resp.body()));
    }

    private static final ConcurrentHashMap<String, long[]> JWKS_TS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, PublicKey>> JWKS_CACHE = new ConcurrentHashMap<>();
    private static final long JWKS_TTL_MS = 3_600_000L;

    public User verifyIdToken(String idToken) {
        if (!notBlank(idToken)) throw new JxException(401, "id_token ausente");
        String kid    = jwtKid(idToken);
        PublicKey key = resolveJwksKey(kid);
        long nowSecs  = System.currentTimeMillis() / 1000L;
        return userFromClaims(verifyJwtClaims(idToken, key, provider.issuer(), provider.clientId(), nowSecs));
    }

    static Map<String, Object> verifyJwtClaims(String jwt, PublicKey key, String issuer,
                                               String audience, long nowSecs) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) throw new JxException(401, "id_token mal formado");

        Map<String, Object> header = asMap(new String(b64urlDecode(parts[0]), StandardCharsets.UTF_8));
        if (!"RS256".equals(str(header.get("alg"))))
            throw new JxException(401, "algoritmo de id_token no soportado: " + str(header.get("alg")));

        byte[] signed = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(signed);
            if (!verifier.verify(b64urlDecode(parts[2])))
                throw new JxException(401, "firma del id_token inválida");
        } catch (JxException e) {
            throw e;
        } catch (Exception e) {
            throw new JxException(401, "no se pudo verificar el id_token: " + e.getMessage());
        }

        Map<String, Object> claims = asMap(new String(b64urlDecode(parts[1]), StandardCharsets.UTF_8));

        String iss = str(claims.get("iss"));
        if (notBlank(issuer) && !issuer.equals(iss)
                && !(issuer.equals("https://accounts.google.com") && "accounts.google.com".equals(iss)))
            throw new JxException(401, "issuer del id_token inválido");

        if (notBlank(audience) && !audience.equals(str(claims.get("aud"))))
            throw new JxException(401, "audience del id_token inválido");

        long exp = asLong(claims.get("exp"));
        if (exp > 0 && nowSecs > exp + 60)
            throw new JxException(401, "id_token expirado");

        return claims;
    }

    private PublicKey resolveJwksKey(String kid) {
        Map<String, PublicKey> keys = jwks(false);
        PublicKey key = kid != null ? keys.get(kid) : null;
        if (key == null) {
            keys = jwks(true);
            key = kid != null ? keys.get(kid)
                : (keys.size() == 1 ? keys.values().iterator().next() : null);
        }
        if (key == null) throw new JxException(401, "clave JWKS no encontrada (kid=" + kid + ")");
        return key;
    }

    private Map<String, PublicKey> jwks(boolean force) {
        String url = provider.jwksUrl();
        long[] ts  = JWKS_TS.get(url);
        long now   = System.currentTimeMillis();
        Map<String, PublicKey> cached = JWKS_CACHE.get(url);
        if (!force && cached != null && ts != null && now - ts[0] < JWKS_TTL_MS) return cached;

        JxHttp.Response resp = JxHttp.get(url);
        if (!resp.isOk()) {
            if (cached != null) return cached;
            throw new JxException(502, "no se pudo obtener el JWKS del proveedor");
        }
        Map<String, PublicKey> map = new LinkedHashMap<>();
        Object keysObj = asMap(resp.body()).get("keys");
        if (keysObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> jwk)) continue;
                try {
                    String kid = str(jwk.get("kid"));
                    String n   = str(jwk.get("n"));
                    String e   = str(jwk.get("e"));
                    if (kid != null && n != null && e != null) map.put(kid, rsaKey(n, e));
                } catch (Exception ignored) {}
            }
        }
        JWKS_CACHE.put(url, map);
        JWKS_TS.put(url, new long[]{now});
        return map;
    }

    private static String jwtKid(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return null;
        return str(asMap(new String(b64urlDecode(parts[0]), StandardCharsets.UTF_8)).get("kid"));
    }

    static PublicKey rsaKey(String modulusB64, String exponentB64) {
        try {
            BigInteger n = new BigInteger(1, b64urlDecode(modulusB64));
            BigInteger e = new BigInteger(1, b64urlDecode(exponentB64));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
        } catch (Exception ex) {
            throw new IllegalStateException("JWK RSA inválido", ex);
        }
    }

    private static byte[] b64urlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static User userFromClaims(Map<String, Object> c) {
        return new User(
                str(c.get("sub")),
                str(c.get("email")),
                asBool(c.get("email_verified")),
                str(c.get("name")),
                str(c.get("given_name")),
                str(c.get("family_name")),
                str(c.get("picture")),
                str(c.get("locale")));
    }

    public static String newToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public static String codeChallengeS256(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

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
