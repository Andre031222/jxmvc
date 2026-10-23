import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SecProbe {
    static String host = "localhost";
    static String virtualHost = "bench.example";
    static int port = 8080;

    record Resp(int status, Map<String, String> headers, String body) {
        String header(String name) { return headers.getOrDefault(name.toLowerCase(Locale.ROOT), ""); }
        boolean is2xx() { return status >= 200 && status < 300; }
        boolean is3xx() { return status >= 300 && status < 400; }
        boolean is4xx() { return status >= 400 && status < 500; }
    }

    record Result(String id, String verdict, String detail) {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0) port = Integer.parseInt(args[0]);
        List<String> skip = args.length > 1 ? List.of(args[1].split(",")) : List.of();
        List<Result> results = new ArrayList<>();

        Resp plain = send("GET", "/plaintext", Map.of(), null);
        results.add(check("H1", plain.header("x-content-type-options").equalsIgnoreCase("nosniff"), "X-Content-Type-Options=" + plain.header("x-content-type-options")));
        boolean frame = !plain.header("x-frame-options").isEmpty() || plain.header("content-security-policy").contains("frame-ancestors");
        results.add(check("H2", frame, "X-Frame-Options=" + plain.header("x-frame-options")));
        results.add(check("H3", !plain.header("referrer-policy").isEmpty(), "Referrer-Policy=" + plain.header("referrer-policy")));
        results.add(check("H4", !plain.header("content-security-policy").isEmpty(), "CSP=" + plain.header("content-security-policy")));
        String server = plain.header("server") + plain.header("x-powered-by");
        results.add(check("H5", !server.matches(".*\\d.*"), "Server=" + plain.header("server") + " X-Powered-By=" + plain.header("x-powered-by")));

        Resp big = send("POST", "/echo", Map.of("Content-Type", "text/plain"), new byte[20 * 1024 * 1024]);
        results.add(check("B1", big.status() == 413 || big.status() == 400 || big.status() == 0, "20 MiB -> " + big.status()));

        Resp deep = send("POST", "/json-echo", Map.of("Content-Type", "application/json"),
                ("{\"a\":" + "[".repeat(100_000) + "]".repeat(100_000) + "}").getBytes(StandardCharsets.UTF_8));
        results.add(skip.contains("B2") ? na("B2") : check("B2", deep.status() == 400 || deep.status() == 413 || deep.status() == 422, "anidamiento 100000 -> " + deep.status()));

        Resp bad = send("POST", "/json-echo", Map.of("Content-Type", "application/json"), "{\"a\":".getBytes(StandardCharsets.UTF_8));
        results.add(skip.contains("B3") ? na("B3") : check("B3", bad.status() == 400 || bad.status() == 422, "JSON truncado -> " + bad.status()));

        Resp boom = send("GET", "/boom", Map.of("Accept", "*/*"), null);
        boolean leak = boom.body().contains("secret-detail-123") || boom.body().contains("IllegalStateException") || boom.body().contains("at bench.");
        results.add(check("E1", !leak, "excepción -> " + boom.status() + (leak ? " (filtra detalle interno)" : "")));

        Resp csrf = send("POST", "/echo", Map.of("Content-Type", "text/plain", "Origin", "https://evil.example",
                "Sec-Fetch-Site", "cross-site", "Cookie", "JSESSIONID=probe"), "a=1".getBytes(StandardCharsets.UTF_8));
        results.add(check("C1", csrf.status() == 403 || csrf.status() == 401 || csrf.status() == 400, "POST entre sitios -> " + csrf.status()));

        Resp go = send("GET", "/go?to=https%3A%2F%2Fevil.example%2F", Map.of(), null);
        boolean open = go.is3xx() && go.header("location").startsWith("https://evil.example");
        results.add(check("R1", !open, "redirect -> " + go.status() + " " + go.header("location")));

        Resp xss = send("GET", "/hello?name=%3Cscript%3Ealert(1)%3C%2Fscript%3E", Map.of(), null);
        results.add(check("X1", !xss.body().contains("<script>alert(1)</script>"), "parámetro reflejado -> " + xss.status()));

        boolean traversal = false;
        StringBuilder td = new StringBuilder();
        for (String p : new String[]{"/..%2f..%2f..%2f..%2fetc%2fpasswd", "/%2e%2e/%2e%2e/%2e%2e/etc/passwd", "/WEB-INF/web.xml", "/..;/WEB-INF/web.xml", "/static/..%2f..%2fWEB-INF/web.xml"}) {
            Resp r = send("GET", p, Map.of(), null);
            td.append(r.status()).append(' ');
            if (r.is2xx() && (r.body().contains("root:") || r.body().contains("<web-app"))) traversal = true;
        }
        results.add(check("T1", !traversal, "rutas con .. -> " + td.toString().trim()));

        Resp trace = send("TRACE", "/plaintext", Map.of("X-Probe", "trace-me"), null);
        results.add(check("M1", !(trace.is2xx() && trace.body().contains("trace-me")), "TRACE -> " + trace.status()));

        List<String> exposed = new ArrayList<>();
        for (String p : new String[]{"/actuator", "/actuator/env", "/actuator/heapdump", "/q/dev", "/q/dev-ui", "/jx/info", "/jx/metrics", "/jx/openapi"}) {
            if (send("GET", p, Map.of(), null).is2xx()) exposed.add(p);
        }
        results.add(check("I1", exposed.isEmpty(), "expuestos: " + (exposed.isEmpty() ? "ninguno" : String.join(" ", exposed))));

        int limited = 0;
        for (int i = 0; i < 300; i++) {
            if (send("POST", "/login", Map.of("Content-Type", "text/plain"), "u=admin&p=x".getBytes(StandardCharsets.UTF_8)).status() == 429) limited++;
        }
        results.add(check("L1", limited > 0, "300 intentos de login -> " + limited + " respuestas 429"));

        Resp hdr = send("GET", "/plaintext", Map.of("X-Big", "a".repeat(200_000)), null);
        results.add(check("L2", !hdr.is2xx(), "cabecera de 200 KB -> " + hdr.status()));

        Resp ct = send("POST", "/echo", Map.of("Content-Type", "text/plain", "Transfer-Encoding", "chunked", "Content-Length", "3"), "0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        results.add(check("S1", !ct.is2xx() || ct.body().startsWith("ok:0"), "TE+CL ambiguos -> " + ct.status()));

        for (Result r : results) System.out.println(r.id() + "\t" + r.verdict() + "\t" + r.detail());
    }

    static Result check(String id, boolean pass, String detail) { return new Result(id, pass ? "PASS" : "FAIL", detail); }

    static Result na(String id) { return new Result(id, "N/A", "sin binding JSON en la app mínima"); }

    static Resp send(String method, String path, Map<String, String> headers, byte[] body) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(15000);
            StringBuilder req = new StringBuilder(method + " " + path + " HTTP/1.1\r\nHost: " + virtualHost + "\r\nConnection: close\r\n");
            headers.forEach((k, v) -> req.append(k).append(": ").append(v).append("\r\n"));
            if (body != null && !headers.containsKey("Content-Length") && !headers.containsKey("Transfer-Encoding"))
                req.append("Content-Length: ").append(body.length).append("\r\n");
            req.append("\r\n");
            OutputStream out = s.getOutputStream();
            try {
                out.write(req.toString().getBytes(StandardCharsets.ISO_8859_1));
                if (body != null) {
                    for (int i = 0; i < body.length; i += 65536) out.write(body, i, Math.min(65536, body.length - i));
                }
                out.flush();
            } catch (IOException ignored) {
            }
            return parse(s.getInputStream());
        } catch (IOException e) {
            return new Resp(0, Map.of(), "");
        }
    }

    static Resp parse(InputStream in) throws IOException {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            for (int n; (n = in.read(buf)) != -1; ) all.write(buf, 0, n);
        } catch (IOException ignored) {
        }
        String raw = all.toString(StandardCharsets.ISO_8859_1);
        int split = raw.indexOf("\r\n\r\n");
        if (!raw.startsWith("HTTP/") || split < 0) return new Resp(0, Map.of(), "");
        String[] lines = raw.substring(0, split).split("\r\n");
        int status = Integer.parseInt(lines[0].split(" ")[1]);
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int c = lines[i].indexOf(':');
            if (c > 0) headers.merge(lines[i].substring(0, c).trim().toLowerCase(Locale.ROOT), lines[i].substring(c + 1).trim(), (a, b) -> a + ", " + b);
        }
        byte[] bytes = all.toByteArray();
        byte[] body = java.util.Arrays.copyOfRange(bytes, split + 4, bytes.length);
        if (headers.getOrDefault("transfer-encoding", "").contains("chunked")) body = dechunk(body);
        return new Resp(status, headers, new String(body, StandardCharsets.UTF_8));
    }

    static byte[] dechunk(byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = 0;
        while (i < b.length) {
            int eol = indexOf(b, i);
            if (eol < 0) break;
            String size = new String(b, i, eol - i, StandardCharsets.ISO_8859_1).split(";")[0].trim();
            int n;
            try { n = Integer.parseInt(size, 16); } catch (NumberFormatException e) { break; }
            if (n == 0) break;
            i = eol + 2;
            out.write(b, i, Math.min(n, b.length - i));
            i += n + 2;
        }
        return out.toByteArray();
    }

    static int indexOf(byte[] b, int from) {
        for (int i = from; i + 1 < b.length; i++) if (b[i] == '\r' && b[i + 1] == '\n') return i;
        return -1;
    }
}
