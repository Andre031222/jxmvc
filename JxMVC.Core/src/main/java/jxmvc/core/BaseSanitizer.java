package jxmvc.core;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class BaseSanitizer {
    private BaseSanitizer() {}

    private static final Pattern P_SCRIPT   = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern P_STYLE    = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern P_IFRAME   = Pattern.compile("(?is)<iframe[^>]*>.*?</iframe>");
    private static final Pattern P_OBJECT   = Pattern.compile("(?is)<object[^>]*>.*?</object>");
    private static final Pattern P_EMBED    = Pattern.compile("(?is)<embed[^>]*>");
    private static final Pattern P_JS_PROTO = Pattern.compile("(?i)javascript\\s*:");
    private static final Pattern P_DATA_URI = Pattern.compile("(?i)data\\s*:\\s*text/html");
    private static final Pattern P_VB_PROTO = Pattern.compile("(?i)vbscript\\s*:");
    private static final Pattern P_ON_EVENT = Pattern.compile("(?i)on[a-z]{2,20}\\s*=");
    private static final Pattern P_TAGS     = Pattern.compile("<[^>]{0,500}>");
    private static final Pattern P_SPACES   = Pattern.compile("\\s{2,}");

    public static String clean(String input) {
        if (input == null) return null;

        String v = decodeRepeatedly(input);
        v = decodeHtmlEntities(v);

        v = P_SCRIPT.matcher(v).replaceAll(" ");
        v = P_STYLE.matcher(v).replaceAll(" ");
        v = P_IFRAME.matcher(v).replaceAll(" ");
        v = P_OBJECT.matcher(v).replaceAll(" ");
        v = P_EMBED.matcher(v).replaceAll(" ");
        v = P_JS_PROTO.matcher(v).replaceAll(" ");
        v = P_DATA_URI.matcher(v).replaceAll(" ");
        v = P_VB_PROTO.matcher(v).replaceAll(" ");
        v = P_ON_EVENT.matcher(v).replaceAll(" ");
        v = P_TAGS.matcher(v).replaceAll(" ");
        v = v.replace("<", " ").replace(">", " ");
        v = P_SPACES.matcher(v).replaceAll(" ").trim();

        return v;
    }

    public static String cleanBasic(String input) {
        if (input == null) return null;
        return input.trim()
                    .replace("<", "")
                    .replace(">", "")
                    .replace("\"", "")
                    .replace("'", "");
    }

    private static String decodeRepeatedly(String input) {
        String current = input;
        for (int i = 0; i < 3; i++) {
            String decoded = decodeOnce(current);
            if (decoded.equals(current)) break;
            current = decoded;
        }
        return current;
    }

    private static String decodeOnce(String input) {
        try { return URLDecoder.decode(input, StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { return input; }
    }

    private static String decodeHtmlEntities(String v) {
        return v.replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&apos;", "'")
                .replace("&amp;",  "&")
                .replace("&#x3c;", "<")
                .replace("&#x3e;", ">")
                .replace("&#60;",  "<")
                .replace("&#62;",  ">")
                .replace("&#x27;", "'")
                .replace("&#x22;", "\"");
    }
}
