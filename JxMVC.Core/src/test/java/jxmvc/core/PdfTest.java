package jxmvc.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class PdfTest {

    static int passed = 0;
    static int failed = 0;

    static void ok(String label) { passed++; System.out.println("  OK  " + label); }
    static void fail(String label, String msg) { failed++; System.out.println("  FAIL " + label + ": " + msg); }
    static void check(String label, boolean cond) { if (cond) ok(label); else fail(label, "false"); }
    static void eq(String label, Object exp, Object got) { if (exp.equals(got)) ok(label); else fail(label, "esperado [" + exp + "] obtenido [" + got + "]"); }

    public static void main(String[] args) throws Exception {
        testCreateRoundtrip();
        testPagination();
        testUnmappableChars();
        testDifferencesAndTJ();
        testToUnicode();
        testObjectStream();
        testIncrementalUpdate();
        testFormAndInlineImage();
        testInfoUtf16();
        testGlyphNames();
        testRejects();
        testCorruptInput();
        testDeepNesting();
        testZipBomb();
        System.out.printf("PdfTest: pass=%d fail=%d%n", passed, failed);
    }

    static final class Pdf {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();

        Pdf() { s("%PDF-1.4\n"); }

        Pdf s(String x) { o.writeBytes(x.getBytes(StandardCharsets.ISO_8859_1)); return this; }

        Pdf obj(int n, String body) { return s(n + " 0 obj\n" + body + "\nendobj\n"); }

        Pdf stream(int n, String dict, byte[] data) {
            s(n + " 0 obj\n<< " + dict + " /Length " + data.length + " >>\nstream\n");
            o.writeBytes(data);
            return s("\nendstream\nendobj\n");
        }

        Pdf stream(int n, String content) { return stream(n, "", content.getBytes(StandardCharsets.ISO_8859_1)); }

        byte[] end(String trailer) { return s("trailer\n<< /Root 1 0 R " + trailer + " >>\n%%EOF\n").o.toByteArray(); }

        byte[] end() { return end(""); }
    }

    static Pdf onePage(String font, String content) {
        return new Pdf()
            .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
            .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            .obj(3, "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>")
            .stream(4, content)
            .obj(5, font);
    }

    static final String HELVETICA = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>";

    static void testCreateRoundtrip() {
        byte[] pdf = JxPdf.create("Constancia ñ", "Hola mundo\nSegunda línea: áéíóú ÑÜ ¿¡ €\fPágina dos (con paréntesis) y \\ barra");
        check("create empieza con %PDF-1.4", new String(pdf, 0, 8, StandardCharsets.ISO_8859_1).equals("%PDF-1.4"));
        var doc = JxPdf.read(pdf);
        eq("create: 2 páginas", 2, doc.pageCount());
        eq("create: texto página 1", "Hola mundo\nSegunda línea: áéíóú ÑÜ ¿¡ €", doc.page(0));
        eq("create: texto página 2 con escapes", "Página dos (con paréntesis) y \\ barra", doc.page(1));
        eq("create: título", "Constancia ñ", doc.info().get("Title"));
        eq("create: producer", "JxMVC JxPdf", doc.info().get("Producer"));
    }

    static void testPagination() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 120; i++) sb.append("Línea ").append(i).append('\n');
        var doc = JxPdf.read(JxPdf.create(null, sb.toString()));
        eq("paginado: 120 líneas → 3 páginas", 3, doc.pageCount());
        check("paginado: conserva el orden", doc.text().contains("Línea 1\nLínea 2") && doc.text().contains("Línea 119"));
        String largo = "palabra ".repeat(40).strip();
        var lines = JxPdf.read(JxPdf.create(null, largo)).page(0).split("\n");
        check("ajuste: línea larga partida", lines.length > 1 && Arrays.stream(lines).allMatch(l -> l.length() <= 90));
    }

    static void testUnmappableChars() {
        eq("fuera de cp1252 → ?", "emoji ? ok", JxPdf.read(JxPdf.create(null, "emoji 😀 ok")).page(0));
    }

    static void testDifferencesAndTJ() {
        String font = "<< /Type /Font /Subtype /Type1 /BaseFont /CMR10 /Encoding << /Differences [65 /aacute /ntilde /fi /uni00E9] >> >>";
        byte[] pdf = onePage(font, "BT /F1 12 Tf 72 700 Td [(Hola)-333(mun)-20(do)]TJ 0 -14 Td (ABCD) Tj ET").end();
        eq("Differences + TJ", "Hola mundo\náñfié", JxPdf.read(pdf).page(0));
    }

    static void testToUnicode() {
        String cmap = "/CIDInit /ProcSet findresource begin 12 dict begin begincmap\n"
            + "1 begincodespacerange <0000> <FFFF> endcodespacerange\n"
            + "1 beginbfchar <0001> <0048> endbfchar\n"
            + "2 beginbfrange <0002> <0004> <0061> <0005> <0006> [<00F1> <20AC>] endbfrange\n"
            + "endcmap end end";
        byte[] pdf = onePage("<< /Type /Font /Subtype /Type0 /BaseFont /Arial /Encoding /Identity-H /ToUnicode 6 0 R >>",
                "BT /F1 12 Tf 72 700 Td <00010002000300040005 0006> Tj ET")
            .stream(6, cmap).end();
        eq("Type0 + ToUnicode", "Habcñ€", JxPdf.read(pdf).page(0));
    }

    static void testObjectStream() {
        String pages = "<< /Type /Pages /Kids [3 0 R] /Count 1 >>";
        String page = "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 6 0 R >> >> /Contents 4 0 R >>";
        String header = "2 0 3 " + (pages.length() + 1) + "\n";
        byte[] data = JxPdf.deflate((header + pages + " " + page).getBytes(StandardCharsets.ISO_8859_1));
        byte[] content = JxPdf.deflate("BT /F1 12 Tf 72 700 Td (Comprimido) Tj ET".getBytes(StandardCharsets.ISO_8859_1));
        byte[] pdf = new Pdf()
            .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
            .stream(4, "/Filter /FlateDecode", content)
            .stream(5, "/Type /ObjStm /N 2 /First " + header.length() + " /Filter /FlateDecode", data)
            .obj(6, HELVETICA)
            .end();
        eq("object stream + FlateDecode", "Comprimido", JxPdf.read(pdf).page(0));
    }

    static void testIncrementalUpdate() {
        byte[] pdf = onePage(HELVETICA, "BT /F1 12 Tf (Viejo) Tj ET").stream(4, "BT /F1 12 Tf (Nuevo) Tj ET").end();
        eq("actualización incremental: gana el último", "Nuevo", JxPdf.read(pdf).page(0));
    }

    static void testFormAndInlineImage() {
        byte[] pdf = new Pdf()
            .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
            .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            .obj(3, "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 5 0 R >> /XObject << /Fm1 6 0 R >> >> /Contents 4 0 R >>")
            .stream(4, "BI /W 2 /H 1 /BPC 8 /CS /G ID ÿ)(\u0000 EI\nq /Fm1 Do Q")
            .obj(5, HELVETICA)
            .stream(6, "/Type /XObject /Subtype /Form /BBox [0 0 100 100]", "BT /F1 12 Tf (Desde formulario) Tj ET".getBytes(StandardCharsets.ISO_8859_1))
            .end();
        eq("XObject de formulario tras imagen en línea", "Desde formulario", JxPdf.read(pdf).page(0));
    }

    static void testInfoUtf16() {
        byte[] pdf = onePage(HELVETICA, "BT /F1 12 Tf (x) Tj ET").obj(9, "<< /Title <FEFF00D100F1> /Author (Jos\\351) >>").end("/Info 9 0 R");
        var info = JxPdf.read(pdf).info();
        eq("Info UTF-16BE", "Ññ", info.get("Title"));
        eq("Info octal", "José", info.get("Author"));
    }

    static void testGlyphNames() {
        eq("glifo Ntilde", "Ñ", JxPdf.glyph("Ntilde"));
        eq("glifo uni00E1", "á", JxPdf.glyph("uni00E1"));
        eq("glifo u1F600", "😀", JxPdf.glyph("u1F600"));
        eq("glifo a.sc", "a", JxPdf.glyph("a.sc"));
        check("glifo desconocido → null", JxPdf.glyph("zzzglyph") == null);
    }

    static void testRejects() {
        expectIae("rechaza no-PDF", "hola".getBytes(StandardCharsets.ISO_8859_1));
        expectIae("rechaza cifrado", onePage(HELVETICA, "BT (x) Tj ET").end("/Encrypt 9 0 R"));
        expectIae("rechaza sin páginas", new Pdf().obj(1, "<< /Type /Catalog >>").end());
    }

    static void testCorruptInput() {
        byte[] base = JxPdf.create("Prueba", "Texto de prueba\nOtra línea\fPágina dos");
        Random rnd = new Random(42);
        int bad = 0;
        for (int i = 0; i < 400; i++) {
            byte[] pdf = i < 200 ? Arrays.copyOf(base, 8 + rnd.nextInt(base.length - 8)) : base.clone();
            if (i >= 200) for (int k = 0; k < 8; k++) pdf[rnd.nextInt(pdf.length)] = (byte) rnd.nextInt(256);
            try { JxPdf.read(pdf); }
            catch (IllegalArgumentException ignored) {}
            catch (Throwable t) { bad++; if (bad == 1) fail("entrada corrupta", t.toString()); }
        }
        check("entrada corrupta: solo IllegalArgumentException", bad == 0);
    }

    static void testDeepNesting() {
        try {
            JxPdf.read(("%PDF-1.4\n1 0 obj " + "[".repeat(200_000)).getBytes(StandardCharsets.ISO_8859_1));
            fail("anidamiento en objetos", "no lanzó");
        } catch (IllegalArgumentException e) {
            ok("anidamiento en objetos sin StackOverflow");
        } catch (Throwable t) {
            fail("anidamiento en objetos", t.toString());
        }
        try {
            String page = JxPdf.read(onePage(HELVETICA, "BT (antes) Tj " + "[".repeat(200_000)).end()).page(0);
            eq("anidamiento en content stream conserva lo previo", "antes", page);
        } catch (Throwable t) {
            fail("anidamiento en content stream", t.toString());
        }
    }

    static void testZipBomb() {
        byte[] bomb = JxPdf.deflate(new byte[JxPdf.MAX_STREAM + 1024]);
        byte[] pdf = new Pdf()
            .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
            .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>")
            .stream(4, "/Filter /FlateDecode", bomb)
            .end();
        expectIae("zip bomb limitado a MAX_STREAM", pdf);
    }

    static void expectIae(String label, byte[] pdf) {
        try {
            JxPdf.read(pdf);
            fail(label, "no lanzó");
        } catch (IllegalArgumentException e) {
            ok(label);
        } catch (Throwable t) {
            fail(label, t.toString());
        }
    }
}
