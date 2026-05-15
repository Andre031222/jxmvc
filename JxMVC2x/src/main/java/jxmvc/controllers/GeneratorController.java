package jxmvc.controllers;

import jxmvc.core.ActionResult;
import jxmvc.core.JxMapping.*;
import jxmvc.service.ProjectZipBuilder;

/**
 * Genera y descarga un proyecto Lux / JxMVC personalizado como ZIP.
 * Accesible desde /home/downloads.
 */
@JxControllerMapping("generate")
public class GeneratorController extends BaseController {

    @JxPostMapping("download")
    public ActionResult download() throws Exception {
        String groupId    = clean(model.param("groupId"),    "com.example");
        String artifactId = clean(model.param("artifactId"), "mi-app");
        String appName    = model.param("appName");
        String db         = model.param("db");

        if (appName == null || appName.isBlank()) appName = artifactId;
        if (db      == null || db.isBlank())      db      = "none";

        // Paquete base: groupId + artifactId limpio (sin guiones ni caracteres especiales)
        String pkgSuffix = artifactId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String pkg       = groupId.replaceAll("[^a-zA-Z0-9.]", "").toLowerCase()
                         + (pkgSuffix.isEmpty() ? "" : "." + pkgSuffix);

        byte[] zip = ProjectZipBuilder.build(groupId, artifactId, pkg, db, appName);
        view.raw(zip, "application/zip", artifactId + "-lux-starter.zip");
        return null;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toLowerCase().replaceAll("[^a-zA-Z0-9.\\-]", "");
    }
}
