package jxmvc.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class JxPdf {

    static final int MAX_STREAM = 64 * 1024 * 1024;
    static final int MAX_DEPTH  = 64;

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final Pattern OBJ    = Pattern.compile("(?<![0-9])(\\d+)\\s+(\\d+)\\s+obj\\b|trailer\\s*<<");
    private static final Map<String, Object> EMPTY = Collections.emptyMap();

    private static final String[] WINANSI = new String[256];
    static {
        for (int i = 32; i < 256; i++) {
            String s = new String(new byte[]{(byte) i}, CP1252);
            if (!s.equals("\uFFFD")) WINANSI[i] = s;
        }
    }

    private JxPdf() {}

    public record Doc(List<String> pages, Map<String, String> info) {
        public int pageCount()     { return pages.size(); }
        public String page(int i)  { return pages.get(i); }
        public String text()       { return String.join("\n\n", pages); }
    }

    public static Doc read(Path file) throws IOException { return read(Files.readAllBytes(file)); }

    public static Doc read(byte[] pdf) {
        if (pdf == null || indexOf(pdf, "%PDF-", 0, Math.min(pdf.length, 1024)) < 0)
            throw new IllegalArgumentException("No es un PDF (falta la cabecera %PDF-)");
        return new Reader(pdf).read();
    }

    private record Name(String v) {}
    private record Op(String v) {}
    private record Ref(int num) {}
    private record Stream(Map<String, Object> dict, byte[] raw) {}

    private static final Object EOF         = new Object();
    private static final Object CLOSE_ARRAY = new Object();
    private static final Object CLOSE_DICT  = new Object();

    private static final class PdfSyntax extends RuntimeException {
        PdfSyntax(String m) { super(m, null, false, false); }
    }

    private static final class Reader {
        final byte[] b;
        final Map<Integer, Object> objs = new HashMap<>();
        final Map<String, Object> trailer = new HashMap<>();
        final Map<Map<String, Object>, Font> fonts = new IdentityHashMap<>();

        Reader(byte[] b) { this.b = b; }

        Doc read() {
            scan();
            expandObjectStreams();
            if (trailer.containsKey("Encrypt")) throw new IllegalArgumentException("PDF cifrado: no soportado");

            Map<String, Object> root = dict(trailer.get("Root"));
            if (!root.containsKey("Pages")) root = findCatalog();
            List<String> pages = new ArrayList<>();
            collect(root.get("Pages"), EMPTY, pages, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            if (pages.isEmpty()) throw new IllegalArgumentException("PDF sin páginas legibles");

            Map<String, String> info = new LinkedHashMap<>();
            dict(trailer.get("Info")).forEach((k, v) -> {
                if (resolve(v) instanceof byte[] s) info.put(k, textString(s));
            });
            return new Doc(List.copyOf(pages), Collections.unmodifiableMap(info));
        }

        void scan() {
            Matcher m = OBJ.matcher(new String(b, StandardCharsets.ISO_8859_1));
            int from = 0;
            while (from < b.length && m.find(from)) {
                boolean isTrailer = m.group(1) == null;
                Lexer lx = new Lexer(b, isTrailer ? m.end() - 2 : m.end(), b.length);
                try {
                    Object o = lx.next(0);
                    if (isTrailer) {
                        if (o instanceof Map<?, ?> d) trailer.putAll(cast(d));
                    } else {
                        if (o instanceof Map<?, ?> d && lx.keyword("stream")) {
                            o = new Stream(cast(d), lx.streamData(cast(d)));
                            if ("XRef".equals(nameOf(cast(d).get("Type")))) trailer.putAll(cast(d));
                        }
                        objs.put(Integer.parseInt(m.group(1)), o);
                    }
                    from = Math.max(lx.p, m.end());
                } catch (RuntimeException e) {
                    from = m.end();
                }
            }
        }

        void expandObjectStreams() {
            for (Object o : new ArrayList<>(objs.values())) {
                if (!(o instanceof Stream s) || !"ObjStm".equals(nameOf(s.dict().get("Type")))) continue;
                byte[] data = decode(s);
                if (data == null) continue;
                int n = Math.max(0, Math.min(intOf(s.dict().get("N")), data.length));
                int first = intOf(s.dict().get("First"));
                try {
                    Lexer lx = new Lexer(data, 0, data.length);
                    int[] nums = new int[n], offs = new int[n];
                    for (int i = 0; i < n; i++) { nums[i] = intOf(lx.next(0)); offs[i] = intOf(lx.next(0)); }
                    for (int i = 0; i < n; i++) {
                        try { objs.putIfAbsent(nums[i], new Lexer(data, first + offs[i], data.length).next(0)); }
                        catch (PdfSyntax ignored) {}
                    }
                } catch (PdfSyntax ignored) {}
            }
        }

        Map<String, Object> findCatalog() {
            for (Object o : objs.values()) {
                Map<String, Object> d = dict(o);
                if ("Catalog".equals(nameOf(d.get("Type")))) return d;
            }
            return EMPTY;
        }

        void collect(Object node, Map<String, Object> res, List<String> out, Set<Object> seen, int depth) {
            Map<String, Object> d = dict(node);
            if (depth > MAX_DEPTH || d.isEmpty() || !seen.add(d)) return;
            if (d.containsKey("Resources")) res = dict(d.get("Resources"));
            if (resolve(d.get("Kids")) instanceof List<?> kids) {
                for (Object k : kids) collect(k, res, out, seen, depth + 1);
            } else if (!"Pages".equals(nameOf(d.get("Type")))) {
                out.add(pageText(d, res));
            }
        }

        String pageText(Map<String, Object> page, Map<String, Object> res) {
            Object c = resolve(page.get("Contents"));
            List<?> parts = c instanceof List<?> l ? l : c == null ? List.of() : List.of(c);
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            for (Object part : parts) {
                if (resolve(part) instanceof Stream s) {
                    byte[] d = decode(s);
                    if (d != null) { all.writeBytes(d); all.write('\n'); }
                }
            }
            Text t = new Text();
            run(all.toByteArray(), res, t, 0);
            return t.toString();
        }

        void run(byte[] data, Map<String, Object> res, Text t, int depth) {
            if (depth > 8) return;
            Map<String, Object> fontRes = dict(res.get("Font"));
            Font font = Font.NONE;
            List<Object> args = new ArrayList<>();
            Lexer lx = new Lexer(data, 0, data.length);
            try {
                for (Object o; (o = lx.next(0)) != EOF; ) {
                    if (!(o instanceof Op op)) { args.add(o); continue; }
                    switch (op.v()) {
                        case "BT" -> { t.lineY = 0; t.scale = 1; t.moved = true; }
                        case "Tf" -> font = font(fontRes.get(nameOf(arg(args, 1))));
                        case "TL" -> t.leading = num(args, 0);
                        case "Td" -> t.move(num(args, 0));
                        case "TD" -> { t.leading = -num(args, 0); t.move(num(args, 0)); }
                        case "Tm" -> { double d = num(args, 2); t.scale = d == 0 ? 1 : d; t.lineY = num(args, 0); t.moved = true; }
                        case "T*" -> t.move(-t.leading);
                        case "Tj" -> t.show(font.decode(bytes(arg(args, 0))));
                        case "'", "\"" -> { t.move(-t.leading); t.show(font.decode(bytes(arg(args, 0)))); }
                        case "TJ" -> {
                            if (arg(args, 0) instanceof List<?> items) {
                                for (Object e : items) {
                                    if (e instanceof byte[] s) t.show(font.decode(s));
                                    else if (e instanceof Number n && n.doubleValue() < -150) t.space();
                                }
                            }
                        }
                        case "q"  -> t.ctmStack.push(t.ctmY);
                        case "Q"  -> { if (!t.ctmStack.isEmpty()) t.ctmY = t.ctmStack.pop(); }
                        case "cm" -> t.ctmY += num(args, 0);
                        case "Do" -> {
                            Object x = resolve(dict(res.get("XObject")).get(nameOf(arg(args, 0))));
                            if (x instanceof Stream s && "Form".equals(nameOf(s.dict().get("Subtype")))) {
                                byte[] d = decode(s);
                                Map<String, Object> r = s.dict().containsKey("Resources") ? dict(s.dict().get("Resources")) : res;
                                if (d != null) run(d, r, t, depth + 1);
                            }
                        }
                        case "ID" -> lx.skipInlineImage();
                        default -> {}
                    }
                    args.clear();
                }
            } catch (PdfSyntax ignored) {}
        }

        Font font(Object ref) {
            Map<String, Object> f = dict(ref);
            return f.isEmpty() ? Font.NONE : fonts.computeIfAbsent(f, this::buildFont);
        }

        Font buildFont(Map<String, Object> f) {
            int width = "Type0".equals(nameOf(resolve(f.get("Subtype")))) ? 2 : 1;
            Map<Integer, String> uni = null;
            if (resolve(f.get("ToUnicode")) instanceof Stream s) {
                byte[] d = decode(s);
                if (d != null) {
                    int[] w = {0};
                    uni = cmap(d, w);
                    if (w[0] > 0) width = Math.min(w[0], 4);
                }
            }
            return new Font(width, uni, width == 1 ? differences(f.get("Encoding")) : null);
        }

        String[] differences(Object enc) {
            if (!(resolve(enc) instanceof Map<?, ?>)) return null;
            if (!(resolve(dict(enc).get("Differences")) instanceof List<?> diffs)) return null;
            String[] table = WINANSI.clone();
            int code = 0;
            for (Object o : diffs) {
                if (o instanceof Number n) code = n.intValue();
                else if (o instanceof Name nm) {
                    String g = glyph(nm.v());
                    if (g != null && code >= 0 && code < 256) table[code] = g;
                    code++;
                }
            }
            return table;
        }

        byte[] decode(Stream s) {
            if (dict(s.dict().get("DecodeParms")).get("Predictor") instanceof Number pr && pr.intValue() > 1) return null;
            Object f = resolve(s.dict().get("Filter"));
            List<?> filters = f instanceof List<?> l ? l : f == null ? List.of() : List.of(f);
            byte[] data = s.raw();
            for (Object o : filters) {
                String n = nameOf(resolve(o));
                if ("FlateDecode".equals(n) || "Fl".equals(n)) data = inflate(data);
                else return null;
            }
            return data;
        }

        Object resolve(Object o) {
            for (int i = 0; o instanceof Ref r && i < MAX_DEPTH; i++) o = objs.get(r.num());
            return o instanceof Ref ? null : o;
        }

        Map<String, Object> dict(Object o) {
            o = resolve(o);
            if (o instanceof Stream s) return s.dict();
            return o instanceof Map<?, ?> m ? cast(m) : EMPTY;
        }
    }

    private static final class Text {
        final StringBuilder sb = new StringBuilder();
        final Deque<Double> ctmStack = new ArrayDeque<>();
        double lineY, scale = 1, leading, ctmY, lastY = Double.NaN;
        boolean moved;

        void move(double dy) { lineY += dy * scale; moved = true; }

        void show(String s) {
            if (s.isEmpty()) return;
            double y = lineY + ctmY;
            if (!Double.isNaN(lastY) && Math.abs(y - lastY) > 1) newline();
            else if (moved) space();
            lastY = y;
            moved = false;
            sb.append(s);
        }

        void space() {
            int n = sb.length();
            if (n > 0 && !Character.isWhitespace(sb.charAt(n - 1))) sb.append(' ');
        }

        void newline() {
            int n = sb.length();
            while (n > 0 && sb.charAt(n - 1) == ' ') sb.setLength(--n);
            if (n > 0 && sb.charAt(n - 1) != '\n') sb.append('\n');
        }

        @Override public String toString() { return sb.toString().strip(); }
    }

    private record Font(int width, Map<Integer, String> uni, String[] simple) {
        static final Font NONE = new Font(1, null, null);

        String decode(byte[] s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + width <= s.length; i += width) {
                int code = 0;
                for (int j = 0; j < width; j++) code = code << 8 | (s[i + j] & 0xFF);
                String u = uni != null ? uni.get(code) : null;
                if (u == null && width == 1) u = (simple != null ? simple : WINANSI)[code];
                if (u != null) sb.append(u);
            }
            return sb.toString();
        }
    }

    static Map<Integer, String> cmap(byte[] data, int[] width) {
        Map<Integer, String> map = new HashMap<>();
        List<Object> args = new ArrayList<>();
        Lexer lx = new Lexer(data, 0, data.length);
        try {
            for (Object o; (o = lx.next(0)) != EOF; ) {
                if (!(o instanceof Op op)) { args.add(o); continue; }
                switch (op.v()) {
                    case "endcodespacerange" -> {
                        for (Object a : args) if (a instanceof byte[] h) width[0] = Math.max(width[0], h.length);
                    }
                    case "endbfchar" -> {
                        for (int i = 0; i + 1 < args.size(); i += 2)
                            if (args.get(i) instanceof byte[] k && args.get(i + 1) instanceof byte[] v)
                                map.put(code(k), utf16(v));
                    }
                    case "endbfrange" -> {
                        for (int i = 0; i + 2 < args.size(); i += 3) {
                            if (!(args.get(i) instanceof byte[] lo) || !(args.get(i + 1) instanceof byte[] hi)) continue;
                            int a = code(lo), z = code(hi);
                            if (z < a || z - a > 0xFFFF) continue;
                            if (args.get(i + 2) instanceof byte[] v) {
                                String base = utf16(v);
                                if (base.isEmpty()) continue;
                                String head = base.substring(0, base.length() - 1);
                                char last = base.charAt(base.length() - 1);
                                for (int c = a; c <= z; c++) map.put(c, head + (char) (last + (c - a)));
                            } else if (args.get(i + 2) instanceof List<?> l) {
                                for (int c = a; c <= z && c - a < l.size(); c++)
                                    if (l.get(c - a) instanceof byte[] v) map.put(c, utf16(v));
                            }
                        }
                    }
                    default -> {}
                }
                args.clear();
            }
        } catch (PdfSyntax ignored) {}
        return map;
    }

    private static final Map<String, String> GLYPHS = pairs(
        "space", " ", "exclam", "!", "quotedbl", "\"", "numbersign", "#", "dollar", "$",
        "percent", "%", "ampersand", "&", "quotesingle", "'", "quoteright", "’",
        "quoteleft", "‘", "parenleft", "(", "parenright", ")", "asterisk", "*", "plus", "+",
        "comma", ",", "hyphen", "-", "period", ".", "slash", "/",
        "zero", "0", "one", "1", "two", "2", "three", "3", "four", "4",
        "five", "5", "six", "6", "seven", "7", "eight", "8", "nine", "9",
        "colon", ":", "semicolon", ";", "less", "<", "equal", "=", "greater", ">",
        "question", "?", "at", "@", "bracketleft", "[", "backslash", "\\", "bracketright", "]",
        "asciicircum", "^", "underscore", "_", "grave", "`", "braceleft", "{", "bar", "|",
        "braceright", "}", "asciitilde", "~", "endash", "–", "emdash", "—",
        "quotedblleft", "“", "quotedblright", "”", "quotedblbase", "„",
        "quotesinglbase", "‚", "bullet", "•", "ellipsis", "…",
        "fi", "fi", "fl", "fl", "ff", "ff", "ffi", "ffi", "ffl", "ffl",
        "exclamdown", "¡", "questiondown", "¿", "ordfeminine", "ª",
        "ordmasculine", "º", "degree", "°", "section", "§", "paragraph", "¶",
        "copyright", "©", "registered", "®", "trademark", "™",
        "Euro", "€", "euro", "€", "sterling", "£", "yen", "¥", "cent", "¢",
        "germandbls", "ß", "ae", "æ", "AE", "Æ", "oe", "œ", "OE", "Œ",
        "oslash", "ø", "Oslash", "Ø", "dotlessi", "ı", "minus", "−",
        "multiply", "×", "divide", "÷", "plusminus", "±", "mu", "µ",
        "periodcentered", "·", "guillemotleft", "«", "guillemotright", "»",
        "dagger", "†", "daggerdbl", "‡", "nbspace", " ");

    private static final Map<String, String> ACCENTS = pairs(
        "acute", "\u0301", "grave", "\u0300", "circumflex", "\u0302", "dieresis", "\u0308",
        "tilde", "\u0303", "cedilla", "\u0327", "ring", "\u030A", "caron", "\u030C");

    static String glyph(String n) {
        String s = GLYPHS.get(n);
        if (s != null) return s;
        if (n.length() == 1) return n;
        if (n.matches("uni([0-9A-Fa-f]{4})+")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < n.length(); i += 4) sb.append((char) Integer.parseInt(n.substring(i, i + 4), 16));
            return sb.toString();
        }
        if (n.matches("u[0-9A-Fa-f]{4,6}")) {
            int cp = Integer.parseInt(n.substring(1), 16);
            return Character.isValidCodePoint(cp) ? Character.toString(cp) : null;
        }
        for (var a : ACCENTS.entrySet())
            if (n.length() == a.getKey().length() + 1 && n.endsWith(a.getKey()))
                return Normalizer.normalize(n.charAt(0) + a.getValue(), Normalizer.Form.NFC);
        int dot = n.indexOf('.');
        return dot > 0 ? glyph(n.substring(0, dot)) : null;
    }

    private static final class Lexer {
        final byte[] b;
        final int end;
        int p;

        Lexer(byte[] b, int p, int end) { this.b = b; this.p = Math.max(0, p); this.end = Math.min(end, b.length); }

        Object next(int depth) {
            if (depth > MAX_DEPTH) throw new PdfSyntax("anidamiento excesivo");
            while (true) {
                skipWs();
                if (p >= end) return EOF;
                int c = b[p] & 0xFF;
                switch (c) {
                    case '(' -> { return literal(); }
                    case '<' -> {
                        if (p + 1 < end && b[p + 1] == '<') { p += 2; return dictBody(depth); }
                        return hex();
                    }
                    case '[' -> { p++; return arrayBody(depth); }
                    case ']' -> { p++; return CLOSE_ARRAY; }
                    case '>' -> {
                        p++;
                        if (p < end && b[p] == '>') { p++; return CLOSE_DICT; }
                    }
                    case ')', '{', '}' -> p++;
                    case '/' -> { return name(); }
                    default -> {
                        if (c == '+' || c == '-' || c == '.' || isDigit(c)) return number();
                        String k = keyword();
                        return switch (k) {
                            case "true"  -> Boolean.TRUE;
                            case "false" -> Boolean.FALSE;
                            case "null"  -> null;
                            default      -> new Op(k);
                        };
                    }
                }
            }
        }

        List<Object> arrayBody(int depth) {
            List<Object> l = new ArrayList<>();
            for (Object o; (o = next(depth + 1)) != EOF && o != CLOSE_ARRAY; ) if (o != CLOSE_DICT) l.add(o);
            return l;
        }

        Map<String, Object> dictBody(int depth) {
            Map<String, Object> d = new LinkedHashMap<>();
            while (true) {
                Object k = next(depth + 1);
                if (k == EOF || k == CLOSE_DICT) return d;
                if (!(k instanceof Name n)) continue;
                Object v = next(depth + 1);
                if (v == EOF || v == CLOSE_DICT) return d;
                d.put(n.v(), v);
            }
        }

        byte[] literal() {
            p++;
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            int depth = 1;
            while (p < end) {
                int c = b[p++] & 0xFF;
                if (c == '\\' && p < end) {
                    int e = b[p++] & 0xFF;
                    switch (e) {
                        case 'n'  -> o.write('\n');
                        case 'r'  -> o.write('\r');
                        case 't'  -> o.write('\t');
                        case 'b'  -> o.write('\b');
                        case 'f'  -> o.write('\f');
                        case '\r' -> { if (p < end && b[p] == '\n') p++; }
                        case '\n' -> {}
                        default -> {
                            if (e >= '0' && e <= '7') {
                                int v = e - '0';
                                for (int i = 0; i < 2 && p < end && b[p] >= '0' && b[p] <= '7'; i++) v = v * 8 + (b[p++] - '0');
                                o.write(v);
                            } else {
                                o.write(e);
                            }
                        }
                    }
                } else if (c == '(') {
                    depth++;
                    o.write(c);
                } else if (c == ')') {
                    if (--depth == 0) break;
                    o.write(c);
                } else {
                    o.write(c);
                }
            }
            return o.toByteArray();
        }

        byte[] hex() {
            p++;
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            int hi = -1;
            while (p < end && b[p] != '>') {
                int d = Character.digit(b[p++], 16);
                if (d < 0) continue;
                if (hi < 0) hi = d;
                else { o.write(hi << 4 | d); hi = -1; }
            }
            if (hi >= 0) o.write(hi << 4);
            p++;
            return o.toByteArray();
        }

        Name name() {
            p++;
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            while (p < end && isRegular(b[p])) {
                if (b[p] == '#' && p + 2 < end) {
                    int h = Character.digit(b[p + 1], 16), l = Character.digit(b[p + 2], 16);
                    if (h >= 0 && l >= 0) { o.write(h << 4 | l); p += 3; continue; }
                }
                o.write(b[p++]);
            }
            return new Name(o.toString(StandardCharsets.ISO_8859_1));
        }

        Object number() {
            int s = p++;
            while (p < end && (isDigit(b[p] & 0xFF) || b[p] == '.')) p++;
            String t = new String(b, s, p - s, StandardCharsets.ISO_8859_1);
            Number n;
            try { n = Long.parseLong(t); }
            catch (NumberFormatException e) {
                try { n = Double.parseDouble(t); } catch (NumberFormatException e2) { n = 0L; }
            }
            if (n instanceof Long num && num >= 0 && isDigit(b[s] & 0xFF)) {
                int save = p;
                skipWs();
                int gs = p;
                while (p < end && isDigit(b[p] & 0xFF)) p++;
                if (p > gs) {
                    skipWs();
                    if (p < end && b[p] == 'R' && (p + 1 >= end || !isRegular(b[p + 1]))) {
                        p++;
                        return new Ref((int) Math.min(num, Integer.MAX_VALUE));
                    }
                }
                p = save;
            }
            return n;
        }

        String keyword() {
            int s = p;
            while (p < end && isRegular(b[p])) p++;
            if (p == s) p++;
            return new String(b, s, p - s, StandardCharsets.ISO_8859_1);
        }

        boolean keyword(String k) {
            int save = p;
            skipWs();
            if (indexOf(b, k, p, Math.min(end, p + k.length())) == p
                    && (p + k.length() >= end || !isRegular(b[p + k.length()]))) {
                p += k.length();
                return true;
            }
            p = save;
            return false;
        }

        byte[] streamData(Map<String, Object> d) {
            if (p < end && b[p] == '\r') p++;
            if (p < end && b[p] == '\n') p++;
            int start = p, stop = -1;
            if (d.get("Length") instanceof Number n) {
                long e = start + n.longValue();
                if (e >= start && e <= end) {
                    p = (int) e;
                    if (keyword("endstream")) stop = (int) e;
                    else p = start;
                }
            }
            if (stop < 0) {
                int i = indexOf(b, "endstream", start, end);
                if (i < 0) throw new PdfSyntax("stream sin endstream");
                p = i + 9;
                stop = i;
                if (stop > start && b[stop - 1] == '\n') stop--;
                if (stop > start && b[stop - 1] == '\r') stop--;
            }
            return Arrays.copyOfRange(b, start, stop);
        }

        void skipInlineImage() {
            for (int i = p + 1; i + 1 < end; i++) {
                if (b[i] == 'E' && b[i + 1] == 'I' && isWs(b[i - 1] & 0xFF) && (i + 2 >= end || isWs(b[i + 2] & 0xFF))) {
                    p = i + 2;
                    return;
                }
            }
            p = end;
        }

        void skipWs() {
            while (p < end) {
                int c = b[p] & 0xFF;
                if (c == '%') { while (p < end && b[p] != '\n' && b[p] != '\r') p++; }
                else if (isWs(c)) p++;
                else return;
            }
        }
    }

    private static final int WRAP = 90, LINES = 52;

    public static byte[] create(String title, String text) {
        List<List<String>> pages = paginate(text == null ? "" : text);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offs = new ArrayList<>();

        write(out, "%PDF-1.4\n%âãÏÓ\n");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(i == 0 ? "" : " ").append(5 + 2 * i).append(" 0 R");
        obj(out, offs, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offs, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        obj(out, offs, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        obj(out, offs, "<< /Producer (JxMVC JxPdf)" + (title != null ? " /Title (" + escape(title) + ")" : "") + " >>");
        for (int i = 0; i < pages.size(); i++) {
            obj(out, offs, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>");
            byte[] content = deflate(pageContent(pages.get(i)));
            offs.add(out.size());
            write(out, offs.size() + " 0 obj\n<< /Length " + content.length + " /Filter /FlateDecode >>\nstream\n");
            out.writeBytes(content);
            write(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        write(out, "xref\n0 " + (offs.size() + 1) + "\n0000000000 65535 f \n");
        for (int o : offs) write(out, String.format("%010d 00000 n \n", o));
        write(out, "trailer\n<< /Size " + (offs.size() + 1) + " /Root 1 0 R /Info 4 0 R >>\nstartxref\n"
                + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static List<List<String>> paginate(String text) {
        List<List<String>> pages = new ArrayList<>();
        for (String chunk : text.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ").split("\f", -1)) {
            List<String> lines = new ArrayList<>();
            for (String line : chunk.split("\n", -1)) lines.addAll(wrap(line));
            for (int i = 0; i < lines.size(); i += LINES) pages.add(lines.subList(i, Math.min(i + LINES, lines.size())));
        }
        return pages;
    }

    private static List<String> wrap(String line) {
        List<String> out = new ArrayList<>();
        while (line.length() > WRAP) {
            int cut = line.lastIndexOf(' ', WRAP);
            if (cut <= 0) cut = WRAP;
            out.add(line.substring(0, cut).stripTrailing());
            line = line.substring(cut).stripLeading();
        }
        out.add(line);
        return out;
    }

    private static byte[] pageContent(List<String> lines) {
        StringBuilder sb = new StringBuilder("BT\n/F1 11 Tf\n14 TL\n56 786 Td\n");
        for (int i = 0; i < lines.size(); i++) sb.append(i == 0 ? "" : "T* ").append('(').append(escape(lines.get(i))).append(") Tj\n");
        return sb.append("ET\n").toString().getBytes(CP1252);
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') sb.append('\\');
            if (c >= 0x20) sb.append(c);
        }
        return sb.toString();
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offs, String body) {
        offs.add(out.size());
        write(out, offs.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) { out.writeBytes(s.getBytes(CP1252)); }

    static byte[] deflate(byte[] in) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(in);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(in.length / 2 + 64);
            byte[] buf = new byte[16384];
            while (!d.finished()) out.write(buf, 0, d.deflate(buf));
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    static byte[] inflate(byte[] in) {
        Inflater inf = new Inflater();
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(in.length * 4L, 1 << 20) + 64);
        try {
            inf.setInput(in);
            byte[] buf = new byte[16384];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0) break;
                if (out.size() + n > MAX_STREAM)
                    throw new IllegalArgumentException("Stream excede " + MAX_STREAM + " bytes descomprimidos");
                out.write(buf, 0, n);
            }
        } catch (DataFormatException ignored) {
        } finally {
            inf.end();
        }
        return out.toByteArray();
    }

    private static String textString(byte[] s) {
        if (s.length >= 2 && (s[0] & 0xFF) == 0xFE && (s[1] & 0xFF) == 0xFF)
            return new String(s, 2, s.length - 2, StandardCharsets.UTF_16BE);
        if (s.length >= 3 && (s[0] & 0xFF) == 0xEF && (s[1] & 0xFF) == 0xBB && (s[2] & 0xFF) == 0xBF)
            return new String(s, 3, s.length - 3, StandardCharsets.UTF_8);
        return new String(s, CP1252);
    }

    private static int code(byte[] k) {
        int c = 0;
        for (int i = 0; i < Math.min(k.length, 4); i++) c = c << 8 | (k[i] & 0xFF);
        return c;
    }

    private static String utf16(byte[] v) { return new String(v, StandardCharsets.UTF_16BE); }

    private static Object arg(List<Object> a, int fromEnd) {
        int i = a.size() - 1 - fromEnd;
        return i >= 0 ? a.get(i) : null;
    }

    private static double num(List<Object> a, int fromEnd) { return arg(a, fromEnd) instanceof Number n ? n.doubleValue() : 0; }
    private static byte[] bytes(Object o)  { return o instanceof byte[] s ? s : null; }
    private static String nameOf(Object o) { return o instanceof Name n ? n.v() : null; }
    private static int intOf(Object o)     { return o instanceof Number n ? n.intValue() : 0; }
    private static boolean isDigit(int c)  { return c >= '0' && c <= '9'; }
    private static boolean isWs(int c)     { return c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32; }

    private static boolean isRegular(byte x) {
        int c = x & 0xFF;
        return !isWs(c) && "()<>[]{}/%".indexOf(c) < 0;
    }

    private static int indexOf(byte[] b, String s, int from, int to) {
        outer:
        for (int i = Math.max(0, from); i + s.length() <= to; i++) {
            for (int j = 0; j < s.length(); j++) if (b[i + j] != s.charAt(j)) continue outer;
            return i;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> m) { return (Map<String, Object>) m; }

    private static Map<String, String> pairs(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }
}
