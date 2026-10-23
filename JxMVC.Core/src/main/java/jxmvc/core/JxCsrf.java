package jxmvc.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class JxCsrf {
    public static final String FIELD  = "_csrf";
    public static final String HEADER = "X-CSRF-Token";

    static final String SESSION_KEY = "jx.csrf.token";

    private static final boolean ENABLED =
            BaseDbResolver.propertyBool("jxmvc.security.csrf", false);

    private static final SecureRandom RANDOM = new SecureRandom();

    private JxCsrf() {}

    static boolean enabled() { return ENABLED; }

    public static String token(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        synchronized (session) {
            String token = (String) session.getAttribute(SESSION_KEY);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(SESSION_KEY, token);
            }
            return token;
        }
    }

    public static String field(HttpServletRequest req) {
        return "<input type=\"hidden\" name=\"" + FIELD + "\" value=\"" + token(req) + "\">";
    }

    static boolean validate(HttpServletRequest req) {
        String verb = req.getMethod().toUpperCase();
        if ("GET".equals(verb) || "HEAD".equals(verb) || "OPTIONS".equals(verb) || "TRACE".equals(verb))
            return true;

        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(SESSION_KEY);
        if (expected == null) return false;

        String presented = req.getHeader(HEADER);
        if (presented == null || presented.isBlank()) presented = req.getParameter(FIELD);
        if (presented == null || presented.isBlank()) return false;

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
