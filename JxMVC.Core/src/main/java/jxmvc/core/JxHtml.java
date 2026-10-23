package jxmvc.core;

public final class JxHtml {
    private JxHtml() {}

    public static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&'  -> sb.append("&amp;");
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                case '/'  -> sb.append("&#47;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
