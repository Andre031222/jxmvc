package jxmvc.controllers;

import jxmvc.core.ActionResult;
import jxmvc.core.JxController;
import jxmvc.core.JxDB;
import jxmvc.core.JxException;
import jxmvc.core.JxJson;
import jxmvc.core.JxProfile;

public abstract class BaseController extends JxController {
    protected JxDB db() { return new JxDB(); }

    protected void requireDev() {
        if (!JxProfile.isDev())
            throw JxException.notFound("Ruta no encontrada");
    }

    protected ActionResult jsonStatus(int status, Object body) {
        view.status(status);
        return json(body);
    }

    protected ActionResult jsonOk(Object data) {
        return json("{\"ok\":true,\"data\":" + JxJson.toJson(data) + "}");
    }

    protected ActionResult jsonError(int status, String message) {
        view.status(status);
        return json("{\"ok\":false,\"error\":" + JxJson.toJson(message) + "}");
    }

    protected String requireParam(String name) {
        String v = model.param(name);
        if (v == null || v.isBlank())
            throw jxmvc.core.JxException.badRequest("Parámetro requerido: " + name);
        return v;
    }

    protected String requireArg(int index) {
        String v = model.arg(index);
        if (v == null || v.isBlank())
            throw jxmvc.core.JxException.badRequest("Argumento de ruta requerido en posición " + index);
        return v;
    }
}
