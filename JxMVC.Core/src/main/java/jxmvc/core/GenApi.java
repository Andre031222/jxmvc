package jxmvc.core;

public final class GenApi {
    private GenApi() {}

    static final class RawJson {
        final String json;
        RawJson(String json) { this.json = json; }
    }

    public static RawJson nested(Object... keyValues) {
        return new RawJson(JsonStr(keyValues));
    }

    public static String JsonStr(Object... keyValues) {
        if (keyValues.length % 2 != 0)
            throw new IllegalArgumentException("JsonStr requiere pares clave-valor");
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(',');
            String key = String.valueOf(keyValues[i]);
            Object val = keyValues[i + 1];
            sb.append('"').append(escapeJson(key)).append("\":").append(toJsonValue(val));
        }
        return sb.append('}').toString();
    }

    public static String Ok(String message) {
        return JsonStr("success", true, "message", message);
    }

    public static String Error(String message) {
        return JsonStr("success", false, "error", message);
    }

    public static String Error(int status, String message) {
        return JsonStr("success", false, "status", status, "error", message);
    }

    public static String JsonArray(DBRowSet rs) {
        if (rs == null || rs.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (DBRow row : rs.result()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(rowToJson(row));
        }
        return sb.append(']').toString();
    }

    public static String JsonRow(DBRow row) {
        if (row == null || row.isEmpty()) return "null";
        return rowToJson(row);
    }

    public static String JsonPaged(JxPageResult page) {
        return JsonStr(
            "total", page.getTotal(),
            "page",  page.getPage(),
            "size",  page.getSize(),
            "pages", page.getTotalPages(),
            "data",  page.getData()
        );
    }

    public static String JsonList(Object... items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(toJsonValue(items[i]));
        }
        return sb.append(']').toString();
    }

    public static String toJson(Object val) {
        return toJsonValue(val);
    }

    private static String rowToJson(DBRow row) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String key : row.KeySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(key)).append("\":").append(toJsonValue(row.Get(key)));
        }
        return sb.append('}').toString();
    }

    static String toJsonValue(Object val) {
        if (val == null)              return "null";
        if (val instanceof RawJson r) return r.json;
        if (val instanceof Boolean)   return val.toString();
        if (val instanceof Number)    return val.toString();
        if (val instanceof DBRow r)   return rowToJson(r);
        if (val instanceof DBRowSet s)return JsonArray(s);
        if (val instanceof JxPageResult p) return JsonPaged(p);
        return '"' + escapeJson(val.toString()) + '"';
    }

    private static String escapeJson(String s) {
        return JxJson.escape(s);
    }
}
