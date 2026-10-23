package jxmvc.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionResult {
    public enum Type { VIEW, TEXT, JSON, REDIRECT }

    private final Type   type;
    private final String payload;
    private int                  httpStatus = 0;
    private Map<String, String>  headers    = null;

    private ActionResult(Type type, String payload) {
        this.type    = type;
        this.payload = payload;
    }

    public static ActionResult view(String viewPath)   { return new ActionResult(Type.VIEW,     viewPath); }
    public static ActionResult text(String body)       { return new ActionResult(Type.TEXT,     body); }
    public static ActionResult json(String rawJson)    { return new ActionResult(Type.JSON,     rawJson); }
    public static ActionResult json(Object obj)        { return new ActionResult(Type.JSON,     JxJson.toJson(obj)); }
    public static ActionResult redirect(String target) { return new ActionResult(Type.REDIRECT, target); }

    public static ActionResult noContent()             { return new ActionResult(Type.JSON, "").status(204); }

    public static ActionResult created(Object obj)     { return json(obj).status(201); }

    public ActionResult status(int code) {
        this.httpStatus = code;
        return this;
    }

    public ActionResult header(String name, String value) {
        if (headers == null) headers = new LinkedHashMap<>();
        headers.put(name, value);
        return this;
    }

    public Type   type()       { return type; }
    public String payload()    { return payload; }
    public int    httpStatus() { return httpStatus; }

    public Map<String, String> headers() {
        return headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
    }

    public boolean hasCustomStatus()  { return httpStatus > 0; }
    public boolean hasCustomHeaders() { return headers != null && !headers.isEmpty(); }
}
