package jxmvc.controllers;

import jxmvc.core.*;
import jxmvc.core.JxMapping.*;
import java.util.List;

@JxControllerMain
@JxControllerMapping("home")
public class HomeController extends BaseController {

    @JxGetMapping("index")
    public ActionResult index() {
        return view("home/index");
    }

    @JxGetMapping("/about")
    public ActionResult about() {
        model.setVar("title",     "About · JxMVC 3.0.0");
        model.setVar("framework", "Lightning-X MVC 3.0.0 (JxMVC)");
        return view("home/about");
    }

    @JxGetMapping("ping")
    public ActionResult ping() {
        return text("pong!");
    }

    @JxGetMapping("docs")
    public ActionResult docs() {
        model.setVar("title", "Docs · JxMVC 3.0.0");
        return view("home/docs");
    }

    @JxGetMapping("errors")
    public ActionResult errors() {
        model.setVar("title", "Pruebas de errores · JxMVC 3.0.0");
        return view("home/errors");
    }

    @JxGetMapping("downloads")
    public ActionResult downloads() {
        model.setVar("title", "Descargas · JxMVC 3.0.0");
        return view("home/downloads");
    }

    @JxGetMapping("bd")
    public ActionResult bd() {
        model.setVar("title", "BD · JxMVC 3.0.0");

        try (JxDB db = db()) {
            String connState = db.isConnected() ? "Conectado" : "Sin conexión";

            DBRowSet rowset = new DBRowSet();
            rowset.add(DBRow.of("id", 1, "nombre", "Alice", "rol", "Admin"));
            rowset.add(DBRow.of("id", 2, "nombre", "Bruno", "rol", "Editor"));
            rowset.add(DBRow.of("id", 3, "nombre", "Carla", "rol", "Viewer"));
            List<DBRow> rows = rowset.asList();

            model.setVar("connState", connState);
            model.setVar("rows",      rows);
            model.setVar("hasRows",   !rows.isEmpty());
        }
        return view("home/bd");
    }

    @JxGetMapping("list")
    public ActionResult list() {
        String page = model.arg(0);
        return text("Listado page=" + (page != null ? page : "1"));
    }

    @JxGetMapping("register")
    public ActionResult register() {
        return text("home/register OK");
    }

    @JxGetMapping("grabar")
    public ActionResult grabar() {
        String valor = model.argRaw(0);
        model.setVar("route",     model.path());
        model.setVar("value",     valor != null ? valor : "sin argumento");
        model.setVar("argsCount", model.argsCount());
        return view("home/grabar");
    }

    @JxGetMapping("error403")
    public ActionResult error403() {
        throw new JxException(403, "Acceso denegado — recurso protegido (demo)");
    }

    @JxGetMapping("error500")
    public ActionResult error500() {
        throw JxException.serverError("Demo interna para probar error 500");
    }

    @JxGetMapping("test")
    public ActionResult test() {
        return text("test0:" + model.argRaw(0));
    }

    // Demo REST (GET absoluto y POST)
    @JxGetMapping("/nee/solo-get")
    public ActionResult soloGet() { return text("solo GET OK"); }

    @JxPostMapping("solo-post")
    public ActionResult soloPost() { return text("solo POST OK"); }
}
