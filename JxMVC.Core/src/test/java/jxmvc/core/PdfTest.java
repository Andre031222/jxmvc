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
        testWriter();
        testTrueType();
        testImages();
        testSignature();
        testLatex();
        testPagination();
        testUnmappableChars();
        testDifferencesAndTJ();
        testToUnicode();
        testObjectStream();
        testIncrementalUpdate();
        testFormAndInlineImage();
        testInfoUtf16();
        testGlyphNames();
        testEncrypted();
        testPasswords();
        testFilters();
        testScanned();
        testSuperscript();
        testSpacing();
        testTexAccents();
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

    static byte[] res(String file) throws Exception {
        try (var in = PdfTest.class.getResourceAsStream("/jxpdf/" + file)) { return in.readAllBytes(); }
    }

    static void testTrueType() throws Exception {
        System.out.println("── fuentes TrueType ──");
        JxPdf pdf = new JxPdf();
        pdf.AddFont("Bricolage", res("BricolageGrotesque-Medium.ttf"));
        pdf.SetFont("Bricolage", 16);
        pdf.TextXY(40, 50, "Año 2026: ñandú, über, «hola» y 10 €");
        pdf.SetFont(JxPdf.HELVETICA, 11);
        pdf.Cell(0, 20, "Texto con fuente base", false, false, 1);
        byte[] bytes = pdf.GetStream();
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        check("ttf: incrustada como CIDFontType2", raw.contains("/CIDFontType2") && raw.contains("/FontFile2"));
        check("ttf: nombre PostScript", raw.contains("/BaseFont /BricolageGrotesque"));
        String t = JxPdf.read(bytes).text();
        check("ttf: texto Unicode recuperable", t.contains("Año 2026: ñandú, über, «hola» y 10 €"));
        check("ttf: mezcla con fuente base", t.contains("Texto con fuente base"));
        try { new JxPdf().AddFont("x", new byte[]{1, 2, 3}); fail("ttf: archivo inválido", "aceptado"); }
        catch (IllegalArgumentException e) { ok("ttf: archivo inválido rechazado"); }
        try { new JxPdf().SetFont("NoExiste", 10); fail("ttf: fuente no registrada", "aceptada"); }
        catch (IllegalArgumentException e) { ok("ttf: fuente no registrada rechazada"); }
    }

    static byte[] image(String format, boolean alpha) throws Exception {
        var img = new java.awt.image.BufferedImage(40, 20, alpha ? java.awt.image.BufferedImage.TYPE_INT_ARGB : java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 40; x++) for (int y = 0; y < 20; y++) img.setRGB(x, y, (alpha && x < 10 ? 0x40 : 0xFF) << 24 | (x * 6) << 16 | (y * 12) << 8 | 0x80);
        var out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    static void testImages() throws Exception {
        System.out.println("── imágenes ──");
        byte[] png = image("png", true), jpg = image("jpg", false);
        JxPdf pdf = new JxPdf();
        pdf.Cell(0, 20, "Antes de la imagen", false, false, 1);
        pdf.Image(png, -1, -1, 200, 0);
        pdf.Image(jpg, 300, 100, 0, 50);
        pdf.Image(png, 40, 300, 80, 40);
        pdf.Cell(0, 20, "Después de la imagen", false, false, 1);
        byte[] bytes = pdf.GetStream();
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        check("imagen: JPEG directo con DCTDecode", raw.contains("/Filter /DCTDecode"));
        check("imagen: PNG con transparencia (SMask)", raw.contains("/SMask"));
        eq("imagen: misma imagen se incrusta una sola vez", 2, raw.split("/Subtype /Image /Width 40", -1).length - 2);
        var doc = JxPdf.read(bytes);
        check("imagen: el texto sigue legible", doc.text().contains("Antes de la imagen") && doc.text().contains("Después de la imagen"));
        check("imagen: página con texto no se marca escaneada", !doc.isScanned(0));
        try { new JxPdf().Image(new byte[]{1, 2, 3}, 0, 0, 10, 10); fail("imagen: formato inválido", "aceptado"); }
        catch (IllegalArgumentException e) { ok("imagen: formato inválido rechazado"); }
    }

    static void testSignature() throws Exception {
        System.out.println("── firma digital ──");
        JxPdf.Signer rsa = JxPdf.Signer.fromPkcs12(res("test-rsa.p12"), "jxmvc-test");
        JxPdf.Signer ec = JxPdf.Signer.fromPkcs12(res("test-ec.p12"), "jxmvc-test");
        JxPdf pdf = new JxPdf();
        pdf.SetFont(JxPdf.HELVETICA_BOLD, 14);
        pdf.TextXY(40, 50, "Constancia firmada");
        pdf.Sign(rsa, "Aprobación", "Puno, Perú");
        byte[] signed = pdf.GetStream();
        var sigs = JxPdf.verify(signed);
        eq("firma: una firma", 1, sigs.size());
        check("firma RSA: válida", sigs.get(0).valid());
        check("firma RSA: cubre todo el documento", sigs.get(0).coversWholeDocument());
        eq("firma: firmante", "JxMVC Test RSA", sigs.get(0).signer());
        eq("firma: motivo con tilde", "Aprobación", sigs.get(0).reason());
        eq("firma: el texto sigue legible", "Constancia firmada", JxPdf.read(signed).page(0).trim());

        byte[] tampered = signed.clone();
        int at = new String(signed, StandardCharsets.ISO_8859_1).indexOf("/Producer (JxMVC");
        tampered[at + 11] = 'X';
        check("firma: alteración detectada", !JxPdf.verify(tampered).get(0).valid());

        byte[] twice = JxPdf.sign(signed, ec, "Visto bueno", null);
        var both = JxPdf.verify(twice);
        eq("firma: dos firmas sucesivas", 2, both.size());
        check("firma EC: ambas válidas", both.stream().allMatch(JxPdf.SignatureInfo::valid));
        eq("firma: solo la última cubre todo", 1, (int) both.stream().filter(JxPdf.SignatureInfo::coversWholeDocument).count());

        byte[] simple = JxPdf.sign(JxPdf.create("Simple", "Documento existente"), rsa, null, null);
        check("firma: PDF existente con tabla xref", JxPdf.verify(simple).get(0).valid());
        eq("firma: sin firmas en PDF sin firmar", 0, JxPdf.verify(JxPdf.create(null, "x")).size());
        try { JxPdf.sign(res("rc4-128.pdf"), rsa, null, null); fail("firma: PDF cifrado", "aceptado"); }
        catch (IllegalArgumentException e) { ok("firma: PDF cifrado rechazado"); }
    }

    static void testLatex() throws Exception {
        System.out.println("── PDF generado con LaTeX ──");
        byte[] tex = res("latex.pdf");
        String t = JxPdf.read(tex).text();
        check("latex: acentos y eñes", t.contains("año, ñandú, acción, pingüino"));
        check("latex: título de sección", t.contains("Prueba de JxPdf"));
        byte[] signed = JxPdf.sign(tex, JxPdf.Signer.fromPkcs12(res("test-rsa.p12"), "jxmvc-test"), "Revisado", null);
        var sigs = JxPdf.verify(signed);
        check("latex: firma sobre xref stream válida", sigs.size() == 1 && sigs.get(0).valid() && sigs.get(0).coversWholeDocument());
        check("latex: sigue legible tras firmar", JxPdf.read(signed).text().contains("ñandú"));
    }

    static void testWriter() throws Exception {
        System.out.println("── escritor JxPdf ──");
        JxPdf pdf = new JxPdf();
        pdf.SetFont(JxPdf.HELVETICA_BOLD, 14);
        pdf.TextXY(40, 40, "Documento Generado con JxPDF (Java)");
        pdf.SetBackColor(new java.awt.Color(220, 230, 242));
        pdf.SetFont(JxPdf.HELVETICA, 12);
        pdf.Cell(150, 30, "Celda con fondo", true, true, 0);
        pdf.SetBackColor(new java.awt.Color(255, 230, 200));
        pdf.Cell(150, 30, "Segunda celda", true, true, 1);
        pdf.Cell(300, 25, "Celda completa sin relleno con salto de línea (ñ)", true, false, 1);
        byte[] bytes = pdf.GetStream();
        var doc = JxPdf.read(bytes);
        eq("escritor: una página", 1, doc.pageCount());
        String t = doc.text();
        check("escritor: título", t.contains("Documento Generado con JxPDF (Java)"));
        check("escritor: celdas en orden", t.indexOf("Celda con fondo") >= 0 && t.indexOf("Celda con fondo") < t.indexOf("Segunda celda"));
        check("escritor: acentos y paréntesis", t.contains("salto de línea (ñ)"));
        check("escritor: dos fuentes", new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("/BaseFont /Helvetica-Bold"));
        JxPdf largo = new JxPdf();
        for (int i = 0; i < 60; i++) largo.Cell(0, 20, "Fila " + i, true, false, 1);
        var docLargo = JxPdf.read(largo.GetStream());
        eq("escritor: salto de página automático", 2, docLargo.pageCount());
        check("escritor: última fila en página 2", docLargo.page(1).contains("Fila 59"));
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("jxpdf", ".pdf");
        pdf.Output(tmp.toString());
        eq("escritor: Output escribe el archivo", bytes.length, (int) java.nio.file.Files.size(tmp));
        java.nio.file.Files.deleteIfExists(tmp);
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

    static byte[] fixture(String name) throws Exception {
        try (var in = PdfTest.class.getResourceAsStream("/jxpdf/" + name + ".pdf")) {
            return in.readAllBytes();
        }
    }

    static final String SECRETO = "Contenido secreto: áéíóú Ñ €\nSegunda línea";

    static void testEncrypted() throws Exception {
        for (String name : new String[]{"rc4-40", "rc4-128", "aes-128", "aes-256-r5", "aes-256"}) {
            var doc = JxPdf.read(fixture(name));
            eq("cifrado " + name + ": texto", SECRETO, doc.page(0));
            eq("cifrado " + name + ": título", "Documento cifrado ñ", doc.info().get("Title"));
        }
    }

    static void testPasswords() throws Exception {
        for (String name : new String[]{"aes-256-clave", "rc4-128-clave"}) {
            byte[] pdf = fixture(name);
            try {
                JxPdf.read(pdf);
                fail(name + " sin contraseña", "no lanzó");
            } catch (IllegalArgumentException e) {
                eq(name + " sin contraseña", "Contraseña incorrecta", e.getMessage());
            }
            eq(name + " con contraseña de usuario", SECRETO, JxPdf.read(pdf, "clave").page(0));
            eq(name + " con contraseña de propietario", SECRETO, JxPdf.read(pdf, "propietario").page(0));
        }
    }

    static void testFilters() throws Exception {
        for (String name : new String[]{"ascii85", "asciihex", "lzw", "runlength", "chain", "predictor"}) {
            eq("filtro " + name, "Filtro correcto: LZW ASCII85 RunLength\naaaaaaaaaaaaaaaaaaaa fin", JxPdf.read(fixture(name)).page(0));
        }
    }

    static void testScanned() {
        byte[] pdf = new Pdf()
            .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
            .obj(2, "<< /Type /Pages /Kids [3 0 R 6 0 R] /Count 2 >>")
            .obj(3, "<< /Type /Page /Parent 2 0 R /Resources << /XObject << /Im1 5 0 R >> >> /Contents 4 0 R >>")
            .stream(4, "q 595 0 0 842 0 0 cm /Im1 Do Q")
            .stream(5, "/Type /XObject /Subtype /Image /Width 1 /Height 1 /ColorSpace /DeviceGray /BitsPerComponent 8", new byte[]{0})
            .obj(6, "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 8 0 R >> >> /Contents 7 0 R >>")
            .stream(7, "BT /F1 12 Tf (Texto) Tj ET")
            .obj(8, HELVETICA)
            .end();
        var doc = JxPdf.read(pdf);
        check("página solo imagen → escaneada", doc.isScanned(0) && doc.page(0).isEmpty());
        check("página con texto → no escaneada", !doc.isScanned(1) && doc.page(1).equals("Texto"));
    }

    static void testSuperscript() {
        byte[] pdf = onePage(HELVETICA, "BT /F1 10 Tf 72 700 Td (Autor) Tj 0 4 Td /F1 6 Tf (1) Tj 0 -4 Td /F1 10 Tf (y otro) Tj 0 -14 Td (Siguiente) Tj ET").end();
        String page = JxPdf.read(pdf).page(0);
        check("superíndice en la misma línea", page.startsWith("Autor") && page.contains("otro\nSiguiente") && page.indexOf('\n') == page.indexOf("\nSiguiente"));
    }

    static final String MONO = "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /FirstChar 32 /LastChar 126 /Widths 9 0 R >>";
    static final String MONO_WIDTHS = "[" + "600 ".repeat(95).strip() + "]";

    static void testSpacing() {
        byte[] glyphs = onePage(MONO, "BT /F1 10 Tf 1 0 0 1 72 700 Tm (H) Tj 6 0 Td (o) Tj 6 0 Td (l) Tj 6 0 Td (a) Tj 12 0 Td (y) Tj ET")
            .obj(9, MONO_WIDTHS).end();
        eq("letras posicionadas una a una", "Hola y", JxPdf.read(glyphs).page(0));
        byte[] justified = onePage(MONO, "BT /F1 10 Tf 72 700 Td [(uno)-160(dos)-30(tres)] TJ ET").obj(9, MONO_WIDTHS).end();
        eq("hueco de 0,16 em separa palabras; 0,03 no", "uno dostres", JxPdf.read(justified).page(0));
        byte[] boxes = onePage(MONO, "q 1 0 0 1 72 700 cm BT /F1 10 Tf (uno) Tj ET Q q 1 0 0 1 200 700 cm BT /F1 10 Tf (dos) Tj ET Q")
            .obj(9, MONO_WIDTHS).end();
        eq("cajas ubicadas con cm", "uno dos", JxPdf.read(boxes).page(0));
    }

    static void testTexAccents() {
        String font = "<< /Type /Font /Subtype /Type1 /BaseFont /CMR10 /FirstChar 16 /LastChar 122 /Widths 9 0 R"
            + " /Encoding << /Differences [16 /dotlessi 19 /acute] >> >>";
        String widths = "[" + "500 ".repeat(107).strip() + "]";
        byte[] pdf = onePage(font, "BT /F1 10 Tf 72 700 Td [(gesti\\023)500(on)-333(cap)111(\\023)389(\\020tulo)] TJ ET").obj(9, widths).end();
        eq("tildes de TeX: ´o y ´ı", "gestión capítulo", JxPdf.read(pdf).page(0));
    }

    static void testRejects() {
        expectIae("rechaza no-PDF", "hola".getBytes(StandardCharsets.ISO_8859_1));
        expectIae("rechaza /Encrypt sin diccionario", onePage(HELVETICA, "BT (x) Tj ET").end("/Encrypt 9 0 R"));
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
