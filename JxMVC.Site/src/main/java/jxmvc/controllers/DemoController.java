package jxmvc.controllers;

import jxmvc.core.*;
import jxmvc.core.JxMapping.*;
import jxmvc.models.TestModel;
import java.nio.file.Paths;

@JxControllerMapping("demo")
public class DemoController extends BaseController {
    private static final String DB_UNAVAILABLE = "Base de datos de demostración no configurada";

    @JxGetMapping("index")
    public String index() {
        view.contentType("text/html;charset=UTF-8");
        return "Demo JxMVC 3.6.1 — <b>funcionando</b>";
    }

    @JxGetMapping("salud")
    public ActionResult saludo(String nome) {
        return text("Hola " + nome);
    }

    @JxGetMapping("pdf")
    public ActionResult pdf() throws Exception {
        JxPdf pdf = new JxPdf();
        pdf.SetFont(JxPdf.HELVETICA_BOLD, 18);
        pdf.TextXY(40, 50, "Documento generado con JxPdf");
        pdf.SetFont(JxPdf.HELVETICA, 10);
        pdf.SetTextColor(110, 110, 115);
        pdf.TextXY(40, 68, "JxMVC " + MainLxServlet.VERSION + " · generado en vivo el "
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        pdf.SetTextColor(0, 0, 0);
        pdf.Ln(14);

        pdf.SetFont(JxPdf.HELVETICA, 12);
        pdf.SetBackColor(220, 230, 242);
        pdf.Cell(150, 30, "Celda con fondo", true, true, 0);
        pdf.SetBackColor(255, 230, 200);
        pdf.Cell(150, 30, "Segunda celda", true, true, 1);
        pdf.Cell(300, 25, "Celda completa sin relleno con salto de línea", true, false, 1);
        pdf.Ln(16);

        pdf.SetFont(JxPdf.HELVETICA_BOLD, 13);
        pdf.Cell(0, 22, "Seguridad por defecto: 18 ataques automáticos", false, false, 1);
        String[][] filas = {
            {"JxMVC", "17 / 18", "1"},
            {"Spring Boot", "9 / 18", "34"},
            {"Micronaut", "9 / 18", "36"},
            {"Quarkus", "8 / 16", "104"},
            {"Javalin", "8 / 16", "22"},
        };
        pdf.SetFont(JxPdf.HELVETICA_BOLD, 11);
        pdf.SetTextColor(255, 255, 255);
        pdf.SetBackColor(0, 113, 227);
        pdf.Cell(200, 24, "Framework", true, true, 0);
        pdf.Cell(150, 24, "Protecciones", true, true, 0);
        pdf.Cell(165, 24, "Librerías en producción", true, true, 1);
        pdf.SetTextColor(0, 0, 0);
        pdf.SetFont(JxPdf.HELVETICA, 11);
        for (int i = 0; i < filas.length; i++) {
            if (i == 0) pdf.SetBackColor(220, 230, 242); else if (i % 2 == 0) pdf.SetBackColor(245, 245, 247); else pdf.SetBackColor(255, 255, 255);
            pdf.Cell(200, 22, filas[i][0], true, true, 0);
            pdf.Cell(150, 22, filas[i][1], true, true, 0);
            pdf.Cell(165, 22, filas[i][2], true, true, 1);
        }
        pdf.Ln(16);

        pdf.SetFont(JxPdf.COURIER, 10);
        pdf.SetBackColor(245, 245, 247);
        String[] codigo = {
            "JxPdf pdf = new JxPdf();",
            "pdf.SetFont(JxPdf.HELVETICA_BOLD, 14);",
            "pdf.TextXY(40, 40, \"Documento\");",
            "pdf.SetBackColor(new Color(220, 230, 242));",
            "pdf.Cell(150, 30, \"Celda\", true, true, 1);",
            "view.inline(pdf.GetStream(), \"application/pdf\", \"demo.pdf\");",
        };
        for (String l : codigo) pdf.Cell(0, 16, l, false, true, 1);
        pdf.Ln(16);

        pdf.SetFont(JxPdf.HELVETICA_OBLIQUE, 9);
        pdf.SetTextColor(110, 110, 115);
        pdf.Cell(0, 14, "Sin librerías externas: JxPdf escribe el PDF directamente con el JDK. Código: github.com/Andre031222/jxmvc", false, false, 1);

        byte[] bytes = pdf.GetStream();
        if ("1".equals(model.param("descargar"))) view.raw(bytes, "application/pdf", "demo-jxpdf.pdf");
        else view.inline(bytes, "application/pdf", "demo-jxpdf.pdf");
        return null;
    }

    @JxGetMapping("ping")
    public ActionResult ping() {
        return json(GenApi.JsonStr("pong", true, "version", "3.6.1"));
    }

    @JxCors(origins = {"http://localhost:5173","http://127.0.0.1:5173"}, hosts = {"127.0.0.1","::1"}, methods = {"GET"})
    @JxGetMapping("cors-local")
    public ActionResult corsLocal() {
        return json("{\"scope\":\"local\",\"ok\":true}");
    }

    @JxCors(origins = {"https://app.ejemplo.com"}, hosts = {"127.0.0.1","::1"}, methods = {"GET"})
    @JxGetMapping("cors-remote")
    public ActionResult corsRemote() {
        return json("{\"scope\":\"remote\",\"ok\":true}");
    }

    @JxCors(origins = {"*"}, methods = {"GET"})
    @JxGetMapping("cors-public")
    public ActionResult corsPublic() {
        return json("{\"scope\":\"public\",\"ok\":true}");
    }

    @JxGetMapping("crear")
    public ActionResult crear() {
        sessionSet("t1", "valor creado");
        return text("creado");
    }

    @JxGetMapping("ver")
    public ActionResult ver() {
        String val = (String) sessionGet("t1");
        return text(val != null ? "t1 = " + val : "nulo");
    }

    @JxGetMapping("/inscrips")
    public void inscrips() {
        requireDev();
        TestModel db = new TestModel();
        if (!db.IsConnected()) { view.print(DB_UNAVAILABLE); return; }

        DBRowSet tbl = db.GetInscrips(null);
        model.setVar("connState", "Conectado");
        model.setVar("tbl",       tbl);
        model.setVar("hasRows",   !tbl.isEmpty());
    }

    @JxGetMapping("persona")
    public ActionResult persona() {
        requireDev();
        String id = model.arg(0);
        if (id == null || id.isBlank()) return json(GenApi.Error("Uso: /demo/persona/{id}"));

        TestModel db = new TestModel();
        if (!db.IsConnected()) return json(GenApi.Error(503, DB_UNAVAILABLE));

        DBRow per = db.FindBy("tblPersonas", "id", id);
        if (per == null) return json(GenApi.Error(404, "Persona no encontrada"));

        return json(GenApi.JsonStr(
            "success",  true,
            "inscrip",  per.Get("id"),
            "correo",   per.Get("Correo"),
            "datos",    per.Get("DatosPers"),
            "telefono", per.Get("Telefono"),
            "tipoprog", per.Get("tipProgra"),
            "programa", per.Get("idPrograma")
        ));
    }

    @JxGetMapping("dbremote")
    public ActionResult dbRemote() {
        requireDev();
        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            return db.isConnected() ? text("Conexión remota OK") : text(DB_UNAVAILABLE);
        }
    }

    @JxGetMapping("test-list")
    public ActionResult testList() {
        requireDev();
        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            if (!db.isConnected()) return text(DB_UNAVAILABLE);
            DBRowSet rows = db.getTable("dicTest", "", "ORDER BY \"Id\"");
            StringBuilder sb = new StringBuilder("Filas: " + rows.size() + "\n");
            for (DBRow row : rows.result()) sb.append(renderRow(row)).append("\n");
            return text(sb.toString());
        }
    }

    @JxGetMapping("test-get")
    public ActionResult testGet() {
        requireDev();
        String raw = model.arg(0);
        if (raw == null || raw.isBlank()) return text("Uso: /demo/test-get/{id}");

        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            if (!db.isConnected()) return text(DB_UNAVAILABLE);
            DBRow row = db.queryRow("SELECT * FROM \"dicTest\" WHERE \"Id\" = ?",
                                    Integer.parseInt(raw));
            return row != null ? text(renderRow(row)) : text("No encontrado id=" + raw);
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser un número entero");
        }
    }

    @JxPostMapping("test-add")
    public ActionResult testAdd() {
        requireDev();
        String dni     = model.arg(0);
        String nombres = model.arg(1);
        if (dni == null || nombres == null) return text("Uso: POST /demo/test-add/{dni}/{nombres}");

        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            if (!db.isConnected()) return text(DB_UNAVAILABLE);
            long id = db.insert("dicTest", DBRow.of("DNI", dni, "Nombres", nombres));
            return text("Insertado Id=" + id);
        }
    }

    @JxPostMapping("test-update")
    public ActionResult testUpdate() {
        requireDev();
        String id      = model.arg(0);
        String nombres = model.arg(1);
        if (id == null || nombres == null) return text("Uso: POST /demo/test-update/{id}/{nombres}");

        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            if (!db.isConnected()) return text(DB_UNAVAILABLE);
            db.update("dicTest", DBRow.of("Nombres", nombres), "\"Id\" = ?", Integer.parseInt(id));
            return text("Actualizado Id=" + id);
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser número entero");
        }
    }

    @JxPostMapping("test-delete")
    public ActionResult testDelete() {
        requireDev();
        String id = model.arg(0);
        if (id == null || id.isBlank()) return text("Uso: POST /demo/test-delete/{id}");

        JxDB db = demoDb();
        if (db == null) return text(DB_UNAVAILABLE);
        try (db) {
            if (!db.isConnected()) return text(DB_UNAVAILABLE);
            db.delete("dicTest", "\"Id\" = ?", Integer.parseInt(id));
            return text("Eliminado Id=" + id);
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser número entero");
        }
    }

    @JxGetMapping("upload")
    public ActionResult uploadForm() { requireDev(); return view("demo/upload"); }

    @JxPostMapping("upload")
    public ActionResult uploadPost() {
        requireDev();
        var saved = model.uploadFile("file", uploadDir(), "pdf,jpg,png");
        return saved != null ? text("OK: " + saved.getFileName()) : text("Error: " + model.lastError());
    }

    @JxGetMapping("testdb")
    public ActionResult testDb() { requireDev(); return view("demo/testdb"); }

    private JxDB demoDb() {
        String url  = BaseDbResolver.property("jxmvc.demo.db.url",  "");
        String user = BaseDbResolver.property("jxmvc.demo.db.user", "");
        String pass = BaseDbResolver.property("jxmvc.demo.db.pass", "");
        if (url.isBlank()) return null;
        return new JxDB(url, user, pass);
    }

    private java.nio.file.Path uploadDir() {
        String base = BaseDbResolver.property("jxmvc.demo.upload.dir",
                System.getProperty("java.io.tmpdir"));
        return Paths.get(base, "jxmvc-uploads");
    }

    private String renderRow(DBRow row) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String k : row.keySet()) {
            if (!first) sb.append(" | ");
            sb.append(k).append("=").append(row.get(k));
            first = false;
        }
        return sb.toString();
    }
}
