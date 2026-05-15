package jxmvc.controllers;

import jxmvc.core.*;
import jxmvc.core.JxMapping.*;
import jxmvc.models.TestModel;
import java.nio.file.Paths;

@JxControllerMapping("demo")
public class DemoController extends BaseController {

    // ── Demo básico ───────────────────────────────────────────────────────

    @JxGetMapping("index")
    public String index() {
        view.contentType("text/html;charset=UTF-8");
        return "Demo JxMVC 3.0.0 — <b>funcionando</b>";
    }

    @JxGetMapping("salud")
    public ActionResult saludo(String nome) {
        return text("Hola " + nome);
    }

    @JxGetMapping("ping")
    public ActionResult ping() {
        return json(GenApi.JsonStr("pong", true, "version", "3.0.0"));
    }

    // ── CORS ──────────────────────────────────────────────────────────────

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

    // ── Sesiones ──────────────────────────────────────────────────────────

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

    // ── Base de datos ─────────────────────────────────────────────────────

    @JxGetMapping("/inscrips")
    public void inscrips() {
        TestModel db = new TestModel();
        if (!db.IsConnected()) { view.print("Error: " + db.getError()); return; }

        DBRowSet tbl = db.GetInscrips(null);
        model.setVar("connState", "Conectado");
        model.setVar("tbl",       tbl);
        model.setVar("hasRows",   !tbl.isEmpty());
    }

    // Demo del patrón v3: FindBy + GenApi.JsonStr (acceso directo sin POJO)
    @JxGetMapping("persona")
    public ActionResult persona() {
        String id = model.argRaw(0);
        if (id == null || id.isBlank()) return json(GenApi.Error("Uso: /demo/persona/{id}"));

        TestModel db = new TestModel();
        if (!db.IsConnected()) return json(GenApi.Error("Sin conexión: " + db.getError()));

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
        try (JxDB db = new JxDB("postgresql://fcd.org.pe:5432/dbtest", "rplm", "finesi++")) {
            return db.isConnected() ? text("Conexión remota OK") : text("ERROR: " + db.getError());
        }
    }

    // ── CRUD seguro (sin SQL injection) ───────────────────────────────────

    @JxGetMapping("test-list")
    public ActionResult testList() {
        try (JxDB db = remoteDb()) {
            if (!db.isConnected()) return text("ERROR: " + db.getError());
            DBRowSet rows = db.getTable("dicTest", "", "ORDER BY \"Id\"");
            StringBuilder sb = new StringBuilder("Filas: " + rows.size() + "\n");
            for (DBRow row : rows.result()) sb.append(renderRow(row)).append("\n");
            return text(sb.toString());
        }
    }

    @JxGetMapping("test-get")
    public ActionResult testGet() {
        String raw = model.argRaw(0);
        if (raw == null || raw.isBlank()) return text("Uso: /demo/test-get/{id}");

        try (JxDB db = remoteDb()) {
            if (!db.isConnected()) return text("ERROR: " + db.getError());
            // Parameterized → sin SQL injection
            DBRow row = db.queryRow("SELECT * FROM \"dicTest\" WHERE \"Id\" = ?",
                                    Integer.parseInt(raw));
            return row != null ? text(renderRow(row)) : text("No encontrado id=" + raw);
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser un número entero");
        }
    }

    @JxGetMapping("test-add")
    public ActionResult testAdd() {
        String dni     = model.argRaw(0);
        String nombres = model.argRaw(1);
        if (dni == null || nombres == null) return text("Uso: /demo/test-add/{dni}/{nombres}");

        try (JxDB db = remoteDb()) {
            if (!db.isConnected()) return text("ERROR: " + db.getError());
            long id = db.insert("dicTest", DBRow.of("DNI", dni, "Nombres", nombres));
            return text("Insertado Id=" + id);
        }
    }

    @JxGetMapping("test-update")
    public ActionResult testUpdate() {
        String id      = model.argRaw(0);
        String nombres = model.argRaw(1);
        if (id == null || nombres == null) return text("Uso: /demo/test-update/{id}/{nombres}");

        try (JxDB db = remoteDb()) {
            if (!db.isConnected()) return text("ERROR: " + db.getError());
            db.update("dicTest", DBRow.of("Nombres", nombres), "\"Id\" = ?", Integer.parseInt(id));
            return text(db.getError() == null || db.getError().isBlank()
                ? "Actualizado Id=" + id : "ERROR: " + db.getError());
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser número entero");
        }
    }

    @JxGetMapping("test-delete")
    public ActionResult testDelete() {
        String id = model.argRaw(0);
        if (id == null || id.isBlank()) return text("Uso: /demo/test-delete/{id}");

        try (JxDB db = remoteDb()) {
            if (!db.isConnected()) return text("ERROR: " + db.getError());
            db.delete("dicTest", "\"Id\" = ?", Integer.parseInt(id));
            return text("Eliminado Id=" + id);
        } catch (NumberFormatException e) {
            throw JxException.badRequest("Id debe ser número entero");
        }
    }

    // ── Subida de archivos ────────────────────────────────────────────────

    @JxGetMapping("upload")
    public ActionResult uploadForm() { return view("demo/upload"); }

    @JxPostMapping("upload")
    public ActionResult uploadPost() {
        var saved = model.uploadFile("file", Paths.get("/tmp/files"), "pdf,jpg,png");
        return saved != null ? text("OK: " + saved.getFileName()) : text("Error: " + model.lastError());
    }

    @JxGetMapping("testdb")
    public ActionResult testDb() { return view("demo/testdb"); }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JxDB remoteDb() {
        return new JxDB("postgresql://fcd.org.pe:5432/dbtest", "rplm", "finesi++");
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
