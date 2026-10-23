package jxmvc.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class JxPdf {

    static final int MAX_STREAM = 64 * 1024 * 1024;
    static final int MAX_DEPTH  = 64;

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final Pattern OBJ    = Pattern.compile("(?<![0-9])(\\d+)\\s+(\\d+)\\s+obj\\b|trailer\\s*<<");
    private static final Map<String, Object> EMPTY = Collections.emptyMap();
    private static final String SPACING_ACCENTS   = "\u00B4`\u02C6^\u02DC~\u00A8\u00B8\u02C7\u02DA\u00AF\u02D8\u02D9";
    private static final String COMBINING_ACCENTS = "\u0301\u0300\u0302\u0302\u0303\u0303\u0308\u0327\u030C\u030A\u0304\u0306\u0307";

    private static final String[] WINANSI = new String[256];
    static {
        for (int i = 32; i < 256; i++) {
            String s = new String(new byte[]{(byte) i}, CP1252);
            if (!s.equals("\uFFFD")) WINANSI[i] = s;
        }
    }

    private JxPdf() {}

    public record Doc(List<String> pages, List<Integer> scanned, Map<String, String> info) {
        public int pageCount()            { return pages.size(); }
        public String page(int i)         { return pages.get(i); }
        public boolean isScanned(int i)   { return scanned.contains(i); }
        public String text()              { return String.join("\n\n", pages); }
    }

    public static Doc read(Path file) throws IOException { return read(Files.readAllBytes(file), ""); }

    public static Doc read(Path file, String password) throws IOException { return read(Files.readAllBytes(file), password); }

    public static Doc read(byte[] pdf) { return read(pdf, ""); }

    public static Doc read(byte[] pdf, String password) {
        if (pdf == null || indexOf(pdf, "%PDF-", 0, Math.min(pdf.length, 1024)) < 0)
            throw new IllegalArgumentException("No es un PDF (falta la cabecera %PDF-)");
        return new Reader(pdf, password == null ? "" : password).read();
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
        final String password;
        final Map<Integer, Object> objs = new HashMap<>();
        final Map<Integer, Integer> gens = new HashMap<>();
        final Map<String, Object> trailer = new HashMap<>();
        final Map<Map<String, Object>, Font> fonts = new IdentityHashMap<>();

        Reader(byte[] b, String password) { this.b = b; this.password = password; }

        Doc read() {
            scan();
            decrypt();
            expandObjectStreams();

            Map<String, Object> root = dict(trailer.get("Root"));
            if (!root.containsKey("Pages")) root = findCatalog();
            List<String> pages = new ArrayList<>();
            List<Integer> scanned = new ArrayList<>();
            collect(root.get("Pages"), EMPTY, pages, scanned, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
            if (pages.isEmpty()) throw new IllegalArgumentException("PDF sin páginas legibles");

            Map<String, String> info = new LinkedHashMap<>();
            dict(trailer.get("Info")).forEach((k, v) -> {
                if (resolve(v) instanceof byte[] s) info.put(k, textString(s));
            });
            return new Doc(List.copyOf(pages), List.copyOf(scanned), Collections.unmodifiableMap(info));
        }

        void decrypt() {
            Object enc = trailer.get("Encrypt");
            if (enc == null) return;
            byte[] id = resolve(trailer.get("ID")) instanceof List<?> l && !l.isEmpty() && resolve(l.get(0)) instanceof byte[] f ? f : new byte[0];
            Crypt c = new Crypt(dict(enc), id, password);
            int skip = enc instanceof Ref r ? r.num() : -1;
            for (Map.Entry<Integer, Object> e : objs.entrySet()) {
                if (e.getKey() != skip) e.setValue(c.object(e.getValue(), e.getKey(), gens.getOrDefault(e.getKey(), 0)));
            }
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
                        int num = Integer.parseInt(m.group(1));
                        objs.put(num, o);
                        gens.put(num, Integer.parseInt(m.group(2)));
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

        void collect(Object node, Map<String, Object> res, List<String> out, List<Integer> scanned, Set<Object> seen, int depth) {
            Map<String, Object> d = dict(node);
            if (depth > MAX_DEPTH || d.isEmpty() || !seen.add(d)) return;
            if (d.containsKey("Resources")) res = dict(d.get("Resources"));
            if (resolve(d.get("Kids")) instanceof List<?> kids) {
                for (Object k : kids) collect(k, res, out, scanned, seen, depth + 1);
            } else if (!"Pages".equals(nameOf(d.get("Type")))) {
                Text t = pageText(d, res);
                String text = t.toString();
                if (text.isEmpty() && t.image) scanned.add(out.size());
                out.add(text);
            }
        }

        Text pageText(Map<String, Object> page, Map<String, Object> res) {
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
            return t;
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
                        case "BT" -> t.begin();
                        case "Tf" -> { font = font(fontRes.get(nameOf(arg(args, 1)))); t.size = Math.abs(num(args, 0)); }
                        case "Tc" -> t.charSpace = num(args, 0);
                        case "Tw" -> t.wordSpace = num(args, 0);
                        case "TL" -> t.leading = num(args, 0);
                        case "Td" -> t.move(num(args, 1), num(args, 0));
                        case "TD" -> { t.leading = -num(args, 0); t.move(num(args, 1), num(args, 0)); }
                        case "Tm" -> t.matrix(num(args, 5), num(args, 2), num(args, 1), num(args, 0));
                        case "T*" -> t.move(0, -t.leading);
                        case "Tj" -> t.show(font, bytes(arg(args, 0)));
                        case "'" -> { t.move(0, -t.leading); t.show(font, bytes(arg(args, 0))); }
                        case "\"" -> {
                            t.wordSpace = num(args, 2);
                            t.charSpace = num(args, 1);
                            t.move(0, -t.leading);
                            t.show(font, bytes(arg(args, 0)));
                        }
                        case "TJ" -> {
                            if (arg(args, 0) instanceof List<?> items) {
                                for (Object e : items) {
                                    if (e instanceof byte[] s) t.show(font, s);
                                    else if (e instanceof Number n) t.x -= n.doubleValue() / 1000 * t.size * t.scaleX;
                                }
                            }
                        }
                        case "q"  -> t.ctmStack.push(new double[]{t.ctmX, t.ctmY});
                        case "Q"  -> {
                            if (!t.ctmStack.isEmpty()) {
                                double[] c = t.ctmStack.pop();
                                t.ctmX = c[0];
                                t.ctmY = c[1];
                            }
                        }
                        case "cm" -> { t.ctmX += num(args, 1); t.ctmY += num(args, 0); }
                        case "Do" -> {
                            Object x = resolve(dict(res.get("XObject")).get(nameOf(arg(args, 0))));
                            String sub = x instanceof Stream s ? nameOf(s.dict().get("Subtype")) : null;
                            if ("Image".equals(sub)) t.image = true;
                            if ("Form".equals(sub)) {
                                Stream s = (Stream) x;
                                byte[] d = decode(s);
                                Map<String, Object> r = s.dict().containsKey("Resources") ? dict(s.dict().get("Resources")) : res;
                                if (d != null) run(d, r, t, depth + 1);
                            }
                        }
                        case "ID" -> { lx.skipInlineImage(); t.image = true; }
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
            boolean composite = "Type0".equals(nameOf(resolve(f.get("Subtype"))));
            int width = composite ? 2 : 1;
            Map<Integer, String> uni = null;
            if (resolve(f.get("ToUnicode")) instanceof Stream s) {
                byte[] d = decode(s);
                if (d != null) {
                    int[] w = {0};
                    uni = cmap(d, w);
                    if (composite && w[0] > 0) width = Math.min(w[0], 4);
                }
            }
            Map<Integer, Double> widths = new HashMap<>();
            double missing, unit = 0.001;
            if (composite) {
                Map<String, Object> cid = resolve(f.get("DescendantFonts")) instanceof List<?> l && !l.isEmpty() ? dict(l.get(0)) : EMPTY;
                missing = cid.get("DW") instanceof Number n ? n.doubleValue() : 1000;
                if (resolve(cid.get("W")) instanceof List<?> w) cidWidths(w, widths);
            } else {
                Map<String, Object> desc = dict(f.get("FontDescriptor"));
                missing = desc.get("MissingWidth") instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : 500;
                int first = intOf(resolve(f.get("FirstChar")));
                if (resolve(f.get("Widths")) instanceof List<?> w)
                    for (int k = 0; k < w.size() && k < 256; k++) widths.put(first + k, numOf(resolve(w.get(k))));
                if ("Type3".equals(nameOf(resolve(f.get("Subtype")))) && resolve(f.get("FontMatrix")) instanceof List<?> m && !m.isEmpty())
                    unit = numOf(resolve(m.get(0)));
            }
            return new Font(width, uni, width == 1 ? differences(f.get("Encoding")) : null, widths, missing, unit);
        }

        void cidWidths(List<?> w, Map<Integer, Double> widths) {
            for (int i = 0; i + 1 < w.size() && widths.size() < 65536; ) {
                int first = intOf(resolve(w.get(i)));
                Object next = resolve(w.get(i + 1));
                if (next instanceof List<?> ws) {
                    for (int k = 0; k < ws.size() && widths.size() < 65536; k++) widths.put(first + k, numOf(resolve(ws.get(k))));
                    i += 2;
                } else if (i + 2 < w.size()) {
                    double v = numOf(resolve(w.get(i + 2)));
                    for (int c = first; c <= intOf(next) && widths.size() < 65536; c++) widths.put(c, v);
                    i += 3;
                } else {
                    break;
                }
            }
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
            Object f = resolve(s.dict().get("Filter"));
            Object p = resolve(s.dict().get("DecodeParms"));
            List<?> filters = f instanceof List<?> l ? l : f == null ? List.of() : List.of(f);
            List<?> parms = p instanceof List<?> l ? l : Collections.nCopies(filters.size(), p);
            byte[] data = s.raw();
            for (int i = 0; i < filters.size(); i++) {
                Map<String, Object> dp = i < parms.size() ? dict(parms.get(i)) : EMPTY;
                data = switch (String.valueOf(nameOf(resolve(filters.get(i))))) {
                    case "FlateDecode", "Fl" -> predict(inflate(data), dp);
                    case "LZWDecode", "LZW" -> predict(lzw(data, !(dp.get("EarlyChange") instanceof Number e) || e.intValue() != 0), dp);
                    case "ASCII85Decode", "A85" -> ascii85(data);
                    case "ASCIIHexDecode", "AHx" -> new Lexer(prepend('<', data), 0, data.length + 1).hex();
                    case "RunLengthDecode", "RL" -> runLength(data);
                    default -> null;
                };
                if (data == null) return null;
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
        final Deque<double[]> ctmStack = new ArrayDeque<>();
        double x, lineX, lineY, scaleX = 1, scale = 1, leading, ctmX, ctmY, size, charSpace, wordSpace;
        double lastY = Double.NaN, lastEndX = Double.NaN, lastHeight, accentFrom = Double.NaN;
        boolean image, accentOverPrev, accentAfterGap;

        void begin() {
            x = lineX = lineY = 0;
            scaleX = scale = 1;
        }

        void matrix(double a, double d, double e, double f) {
            scaleX = a == 0 ? 1 : a;
            scale = d == 0 ? 1 : d;
            x = lineX = e;
            lineY = f;
        }

        void move(double tx, double ty) {
            lineX += tx * scaleX;
            lineY += ty * scale;
            x = lineX;
        }

        void show(Font font, byte[] s) {
            String text = font.decode(s);
            double advance = font.advance(s, size, charSpace, wordSpace) * scaleX;
            double at = x + ctmX;
            if (!text.isEmpty()) {
                double y = lineY + ctmY, height = size * Math.abs(scale), em = size * Math.abs(scaleX);
                double tolerance = Math.max(height, lastHeight) > 0 ? 0.5 * Math.max(height, lastHeight) : 1;
                boolean sameLine = Double.isNaN(lastY) || Math.abs(y - lastY) <= tolerance;
                boolean started = !Double.isNaN(lastEndX);
                if (sameLine && !Double.isNaN(accentFrom) && Character.isLetter(text.charAt(0))
                        && at < lastEndX + (accentOverPrev ? 0 : 0.05 * em)) {
                    char mark = sb.charAt(sb.length() - 1);
                    sb.setLength(sb.length() - 1);
                    if (at - accentFrom > 0.1 * em) space();
                    text = compose(text.charAt(0), SPACING_ACCENTS.indexOf(mark)) + text.substring(1);
                    accentFrom = Double.NaN;
                } else {
                    settle();
                    if (!sameLine) {
                        newline();
                    } else if (started && text.length() == 1 && SPACING_ACCENTS.indexOf(text.charAt(0)) >= 0) {
                        accentFrom = lastEndX;
                        accentOverPrev = at < lastEndX - 0.05 * em;
                        accentAfterGap = at - lastEndX > 0.1 * em;
                    } else if (started && Math.abs(at - lastEndX) > 0.1 * em && (at > lastEndX || at < lastEndX - em)) {
                        space();
                    }
                }
                sb.append(text);
                if (Double.isNaN(accentFrom) && text.length() > 1 && SPACING_ACCENTS.indexOf(text.charAt(text.length() - 1)) >= 0) {
                    accentFrom = at + advance;
                    accentOverPrev = accentAfterGap = false;
                }
                lastY = y;
                lastHeight = height;
            }
            x += advance;
            lastEndX = at + advance;
        }

        void settle() {
            if (Double.isNaN(accentFrom)) return;
            accentFrom = Double.NaN;
            int n = sb.length();
            char mark = sb.charAt(n - 1);
            sb.setLength(n - 1);
            if (accentOverPrev && n >= 2 && Character.isLetter(sb.charAt(n - 2))) {
                char base = sb.charAt(n - 2);
                sb.setLength(n - 2);
                sb.append(compose(base, SPACING_ACCENTS.indexOf(mark)));
            } else {
                if (accentAfterGap) space();
                sb.append(mark);
            }
        }

        static String compose(char base, int mark) {
            char b = base == '\u0131' ? 'i' : base == '\u0237' ? 'j' : base;
            return Normalizer.normalize(b + COMBINING_ACCENTS.substring(mark, mark + 1), Normalizer.Form.NFC);
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

        @Override public String toString() {
            settle();
            return sb.toString().strip();
        }
    }

    private record Font(int width, Map<Integer, String> uni, String[] simple, Map<Integer, Double> widths, double missing, double unit) {
        static final Font NONE = new Font(1, null, null, Map.of(), 500, 0.001);

        int code(byte[] s, int i) {
            int c = 0;
            for (int j = 0; j < width; j++) c = c << 8 | (s[i + j] & 0xFF);
            return c;
        }

        String decode(byte[] s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + width <= s.length; i += width) {
                int code = code(s, i);
                String u = uni != null ? uni.get(code) : null;
                if (u == null && width == 1) u = (simple != null ? simple : WINANSI)[code];
                if (u != null) sb.append(u);
            }
            return sb.toString();
        }

        double advance(byte[] s, double size, double charSpace, double wordSpace) {
            if (s == null) return 0;
            double total = 0;
            for (int i = 0; i + width <= s.length; i += width) {
                int code = code(s, i);
                total += widths.getOrDefault(code, missing) * unit * size + charSpace + (width == 1 && code == 32 ? wordSpace : 0);
            }
            return total;
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
        "oslash", "ø", "Oslash", "Ø", "dotlessi", "\u0131", "minus", "−",
        "multiply", "×", "divide", "÷", "plusminus", "±", "mu", "µ",
        "periodcentered", "·", "guillemotleft", "«", "guillemotright", "»",
        "dagger", "†", "daggerdbl", "‡", "nbspace", " ",
        "acute", "\u00B4", "circumflex", "\u02C6", "tilde", "\u02DC", "dieresis", "\u00A8", "cedilla", "\u00B8",
        "caron", "\u02C7", "ring", "\u02DA", "macron", "\u00AF", "breve", "\u02D8", "dotaccent", "\u02D9");

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

    private static final class Crypt {
        static final int NONE = 0, RC4 = 1, AES = 2, AES256 = 3;
        static final byte[] PAD = {
            0x28, (byte) 0xBF, 0x4E, 0x5E, 0x4E, 0x75, (byte) 0x8A, 0x41, 0x64, 0x00, 0x4E, 0x56, (byte) 0xFF, (byte) 0xFA, 0x01, 0x08,
            0x2E, 0x2E, 0x00, (byte) 0xB6, (byte) 0xD0, 0x68, 0x3E, (byte) 0x80, 0x2F, 0x0C, (byte) 0xA9, (byte) 0xFE, 0x64, 0x53, 0x69, 0x7A };

        final boolean metadata;
        final byte[] key;
        final int stm, str;

        Crypt(Map<String, Object> enc, byte[] id, String password) {
            String filter = nameOf(enc.get("Filter"));
            int v = intOf(enc.get("V")), r = intOf(enc.get("R"));
            byte[] o = bytes(enc.get("O")), u = bytes(enc.get("U"));
            if (filter == null || o == null || u == null) throw new IllegalArgumentException("Diccionario /Encrypt incompleto");
            if (!filter.equals("Standard")) throw new IllegalArgumentException("Cifrado con certificado: no soportado");
            metadata = !Boolean.FALSE.equals(enc.get("EncryptMetadata"));
            byte[] k;
            try {
                k = r >= 5
                    ? key256(password.getBytes(StandardCharsets.UTF_8), o, u, bytes(enc.get("OE")), bytes(enc.get("UE")), r)
                    : legacy(password.getBytes(CP1252), o, u, intOf(enc.get("P")), id, r,
                             r == 2 ? 5 : Math.max(5, Math.min(16, intOr(enc.get("Length"), 40) / 8)));
            } catch (GeneralSecurityException e) {
                throw new IllegalArgumentException("Cifrado no soportado por la JVM: " + e.getMessage());
            }
            key = k;
            stm = v >= 4 ? method(enc, "StmF") : RC4;
            str = v >= 4 ? method(enc, "StrF") : RC4;
        }

        Object object(Object o, int num, int gen) {
            if (o instanceof byte[] s) return apply(str, s, num, gen);
            if (o instanceof List<?> l) {
                List<Object> r = new ArrayList<>(l.size());
                for (Object x : l) r.add(object(x, num, gen));
                return r;
            }
            if (o instanceof Map<?, ?> m) {
                Map<String, Object> r = new LinkedHashMap<>();
                cast(m).forEach((k, v) -> r.put(k, object(v, num, gen)));
                return r;
            }
            if (o instanceof Stream s) {
                String type = nameOf(s.dict().get("Type"));
                if ("XRef".equals(type)) return s;
                byte[] raw = "Metadata".equals(type) && !metadata ? s.raw() : apply(stm, s.raw(), num, gen);
                return new Stream(cast((Map<?, ?>) object(s.dict(), num, gen)), raw);
            }
            return o;
        }

        byte[] apply(int method, byte[] data, int num, int gen) {
            try {
                return switch (method) {
                    case RC4    -> rc4(objectKey(num, gen, false), data);
                    case AES    -> aes(objectKey(num, gen, true), data);
                    case AES256 -> aes(key, data);
                    default     -> data;
                };
            } catch (GeneralSecurityException e) {
                return new byte[0];
            }
        }

        byte[] objectKey(int num, int gen, boolean aes) throws GeneralSecurityException {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key);
            md.update(new byte[]{(byte) num, (byte) (num >> 8), (byte) (num >> 16), (byte) gen, (byte) (gen >> 8)});
            if (aes) md.update(new byte[]{0x73, 0x41, 0x6C, 0x54});
            return Arrays.copyOf(md.digest(), Math.min(key.length + 5, 16));
        }

        byte[] legacy(byte[] pw, byte[] o, byte[] u, int p, byte[] id, int r, int len) throws GeneralSecurityException {
            byte[] k = userKey(pad(pw), o, p, id, r, len);
            if (userMatches(k, u, id, r)) return k;
            byte[] ok = digest("MD5", pad(pw));
            if (r >= 3) for (int i = 0; i < 50; i++) ok = digest("MD5", ok);
            ok = Arrays.copyOf(ok, len);
            byte[] x = Arrays.copyOf(o, 32);
            if (r == 2) x = rc4(ok, x);
            else for (int i = 19; i >= 0; i--) x = rc4(xor(ok, i), x);
            k = userKey(x, o, p, id, r, len);
            if (userMatches(k, u, id, r)) return k;
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        byte[] userKey(byte[] pw32, byte[] o, int p, byte[] id, int r, int len) throws GeneralSecurityException {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pw32);
            md.update(o, 0, Math.min(32, o.length));
            md.update(new byte[]{(byte) p, (byte) (p >> 8), (byte) (p >> 16), (byte) (p >> 24)});
            md.update(id);
            if (r >= 4 && !metadata) md.update(new byte[]{-1, -1, -1, -1});
            byte[] h = md.digest();
            if (r >= 3) for (int i = 0; i < 50; i++) h = digest("MD5", Arrays.copyOf(h, len));
            return Arrays.copyOf(h, len);
        }

        static boolean userMatches(byte[] k, byte[] u, byte[] id, int r) throws GeneralSecurityException {
            if (r == 2) return Arrays.equals(rc4(k, PAD), Arrays.copyOf(u, 32));
            byte[] x = rc4(k, digest("MD5", PAD, id));
            for (int i = 1; i <= 19; i++) x = rc4(xor(k, i), x);
            return Arrays.equals(x, Arrays.copyOf(u, 16));
        }

        static byte[] key256(byte[] pw, byte[] o, byte[] u, byte[] oe, byte[] ue, int r) throws GeneralSecurityException {
            if (o.length < 48 || u.length < 48 || oe == null || ue == null) throw new IllegalArgumentException("Diccionario /Encrypt incompleto");
            byte[] p = Arrays.copyOf(pw, Math.min(127, pw.length)), u48 = Arrays.copyOf(u, 48), none = new byte[0];
            if (Arrays.equals(hash(p, Arrays.copyOfRange(u, 32, 40), none, r), Arrays.copyOf(u, 32)))
                return aesRaw(hash(p, Arrays.copyOfRange(u, 40, 48), none, r), ue);
            if (Arrays.equals(hash(p, Arrays.copyOfRange(o, 32, 40), u48, r), Arrays.copyOf(o, 32)))
                return aesRaw(hash(p, Arrays.copyOfRange(o, 40, 48), u48, r), oe);
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        static byte[] hash(byte[] pw, byte[] salt, byte[] udata, int r) throws GeneralSecurityException {
            byte[] k = digest("SHA-256", pw, salt, udata);
            if (r == 5) return k;
            byte[] e = null;
            for (int i = 0; i < 64 || (e[e.length - 1] & 0xFF) > i - 32; i++) {
                byte[] one = concat(pw, k, udata), k1 = new byte[one.length * 64];
                for (int j = 0; j < 64; j++) System.arraycopy(one, 0, k1, j * one.length, one.length);
                Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
                c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, 0, 16, "AES"), new IvParameterSpec(k, 16, 16));
                e = c.doFinal(k1);
                int mod = 0;
                for (int j = 0; j < 16; j++) mod += e[j] & 0xFF;
                k = digest(mod % 3 == 0 ? "SHA-256" : mod % 3 == 1 ? "SHA-384" : "SHA-512", e);
            }
            return Arrays.copyOf(k, 32);
        }

        static int method(Map<String, Object> enc, String which) {
            String name = nameOf(enc.get(which));
            if (name == null || name.equals("Identity")) return NONE;
            Object cf = enc.get("CF") instanceof Map<?, ?> m ? m.get(name) : null;
            return switch (String.valueOf(cf instanceof Map<?, ?> d ? nameOf(d.get("CFM")) : null)) {
                case "V2"    -> RC4;
                case "AESV2" -> AES;
                case "AESV3" -> AES256;
                default      -> NONE;
            };
        }

        static byte[] rc4(byte[] k, byte[] data) throws GeneralSecurityException {
            Cipher c = Cipher.getInstance("ARCFOUR");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "ARCFOUR"));
            return c.doFinal(data);
        }

        static byte[] aes(byte[] k, byte[] data) throws GeneralSecurityException {
            if (data.length < 32 || data.length % 16 != 0) return new byte[0];
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(data, 0, 16));
            return c.doFinal(data, 16, data.length - 16);
        }

        static byte[] aesRaw(byte[] k, byte[] data) throws GeneralSecurityException {
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(new byte[16]));
            return c.doFinal(Arrays.copyOf(data, 32));
        }

        static byte[] pad(byte[] pw) {
            byte[] r = new byte[32];
            int n = Math.min(32, pw.length);
            System.arraycopy(pw, 0, r, 0, n);
            System.arraycopy(PAD, 0, r, n, 32 - n);
            return r;
        }

        static byte[] xor(byte[] k, int i) {
            byte[] r = new byte[k.length];
            for (int j = 0; j < k.length; j++) r[j] = (byte) (k[j] ^ i);
            return r;
        }

        static byte[] digest(String alg, byte[]... parts) throws GeneralSecurityException {
            MessageDigest md = MessageDigest.getInstance(alg);
            for (byte[] p : parts) md.update(p);
            return md.digest();
        }

        static byte[] concat(byte[]... parts) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] p : parts) out.writeBytes(p);
            return out.toByteArray();
        }
    }

    static byte[] predict(byte[] data, Map<String, Object> dp) {
        int predictor = intOr(dp.get("Predictor"), 1);
        if (predictor < 2) return data;
        int colors = intOr(dp.get("Colors"), 1), bpc = intOr(dp.get("BitsPerComponent"), 8), columns = intOr(dp.get("Columns"), 1);
        int bpp = Math.max(1, colors * bpc / 8), row = (colors * bpc * columns + 7) / 8;
        if (row <= 0 || row > MAX_STREAM) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        byte[] prev = new byte[row], cur = new byte[row];
        if (predictor == 2) {
            if (bpc != 8) return data;
            for (int i = 0; i + row <= data.length; i += row) {
                System.arraycopy(data, i, cur, 0, row);
                for (int k = bpp; k < row; k++) cur[k] += cur[k - bpp];
                out.write(cur, 0, row);
            }
            return out.toByteArray();
        }
        for (int i = 0; i + 1 + row <= data.length; i += row + 1) {
            int type = data[i] & 0xFF;
            System.arraycopy(data, i + 1, cur, 0, row);
            for (int k = 0; k < row; k++) {
                int a = k >= bpp ? cur[k - bpp] & 0xFF : 0, b = prev[k] & 0xFF, c = k >= bpp ? prev[k - bpp] & 0xFF : 0;
                int x = cur[k] & 0xFF;
                cur[k] = (byte) (switch (type) {
                    case 1  -> x + a;
                    case 2  -> x + b;
                    case 3  -> x + (a + b) / 2;
                    case 4  -> x + paeth(a, b, c);
                    default -> x;
                });
            }
            out.write(cur, 0, row);
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        return out.toByteArray();
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
    }

    static byte[] lzw(byte[] in, boolean early) {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(in.length * 3L, 1 << 20) + 64);
        List<byte[]> table = new ArrayList<>(4096);
        for (int i = 0; i < 256; i++) table.add(new byte[]{(byte) i});
        table.add(null);
        table.add(null);
        int bits = 9, nbits = 0;
        long buf = 0;
        byte[] prev = null;
        for (byte x : in) {
            buf = (buf << 8) | (x & 0xFF);
            nbits += 8;
            while (nbits >= bits) {
                int code = (int) (buf >>> (nbits - bits)) & ((1 << bits) - 1);
                nbits -= bits;
                buf &= (1L << nbits) - 1;
                if (code == 256) { table.subList(258, table.size()).clear(); bits = 9; prev = null; continue; }
                if (code == 257) return out.toByteArray();
                byte[] entry;
                if (code < table.size() && table.get(code) != null) entry = table.get(code);
                else if (code == table.size() && prev != null) { entry = Arrays.copyOf(prev, prev.length + 1); entry[prev.length] = prev[0]; }
                else return out.toByteArray();
                limit(out, entry.length);
                out.write(entry, 0, entry.length);
                if (prev != null && table.size() < 4096) {
                    byte[] e = Arrays.copyOf(prev, prev.length + 1);
                    e[prev.length] = entry[0];
                    table.add(e);
                }
                prev = entry;
                if (table.size() + (early ? 1 : 0) >= (1 << bits) && bits < 12) bits++;
            }
        }
        return out.toByteArray();
    }

    static byte[] ascii85(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.length);
        long acc = 0;
        int n = 0;
        for (int i = in.length > 1 && in[0] == '<' && in[1] == '~' ? 2 : 0; i < in.length; i++) {
            int c = in[i] & 0xFF;
            if (c == '~') break;
            if (c == 'z' && n == 0) { out.writeBytes(new byte[4]); continue; }
            if (c < '!' || c > 'u') continue;
            acc = acc * 85 + (c - '!');
            if (++n == 5) {
                for (int k = 3; k >= 0; k--) out.write((int) (acc >>> (8 * k)));
                acc = 0;
                n = 0;
            }
        }
        if (n > 1) {
            for (int k = n; k < 5; k++) acc = acc * 85 + 84;
            for (int k = 3; k >= 5 - n; k--) out.write((int) (acc >>> (8 * k)));
        }
        return out.toByteArray();
    }

    static byte[] runLength(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.length);
        for (int i = 0; i < in.length; ) {
            int l = in[i++] & 0xFF;
            if (l == 128) break;
            if (l < 128) {
                int n = Math.min(l + 1, in.length - i);
                out.write(in, i, n);
                i += n;
            } else if (i < in.length) {
                limit(out, 257 - l);
                for (int k = 0; k < 257 - l; k++) out.write(in[i]);
                i++;
            }
        }
        return out.toByteArray();
    }

    private static void limit(ByteArrayOutputStream out, int more) {
        if (out.size() + (long) more > MAX_STREAM) throw new IllegalArgumentException("Stream excede " + MAX_STREAM + " bytes descomprimidos");
    }

    private static byte[] prepend(char c, byte[] data) {
        byte[] r = new byte[data.length + 1];
        r[0] = (byte) c;
        System.arraycopy(data, 0, r, 1, data.length);
        return r;
    }

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

    private static double num(List<Object> a, int fromEnd) { return numOf(arg(a, fromEnd)); }
    private static double numOf(Object o)  { return o instanceof Number n ? n.doubleValue() : 0; }
    private static byte[] bytes(Object o)  { return o instanceof byte[] s ? s : null; }
    private static String nameOf(Object o) { return o instanceof Name n ? n.v() : null; }
    private static int intOf(Object o)     { return o instanceof Number n ? n.intValue() : 0; }
    private static int intOr(Object o, int d) { return o instanceof Number n ? n.intValue() : d; }
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
