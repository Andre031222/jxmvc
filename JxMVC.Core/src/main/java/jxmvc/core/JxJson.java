/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializador JSON ligero — cero dependencias externas.
 * Soporta: null, String, Number, Boolean, DBRow, DBRowSet,
 *          List, Map y cualquier POJO con getters públicos.
 *
 * <pre>
 *   String json = JxJson.toJson(dbRowSet);
 *   String json = JxJson.toJson(Map.of("ok", true, "count", 5));
 * </pre>
 */
public final class JxJson {

    private JxJson() {}

    // ── Deserialización ───────────────────────────────────────────────────

    /**
     * Deserializa JSON a un POJO, tipo primitivo boxeado, {@link Map} o {@link List}.
     *
     * <pre>
     *   UserDto dto  = JxJson.fromJson(body, UserDto.class);
     *   List&lt;?&gt; list = JxJson.fromJson(body, List.class);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        Object parsed = new JsonParser(json.trim()).parse();
        if (parsed == null) return null;

        if (type == String.class)
            return type.cast(parsed instanceof String ? parsed : String.valueOf(parsed));
        if (type == Integer.class || type == int.class)
            return (T) Integer.valueOf(((Number) parsed).intValue());
        if (type == Long.class    || type == long.class)
            return (T) Long.valueOf(((Number) parsed).longValue());
        if (type == Double.class  || type == double.class)
            return (T) Double.valueOf(((Number) parsed).doubleValue());
        if (type == Float.class   || type == float.class)
            return (T) Float.valueOf(((Number) parsed).floatValue());
        if (type == Boolean.class || type == boolean.class)
            return (T) Boolean.valueOf(String.valueOf(parsed));
        if (type == Map.class  || type == LinkedHashMap.class)
            return (T) parsed;
        if (type == List.class || type == ArrayList.class)
            return (T) parsed;
        if (parsed instanceof Map<?, ?> map)
            return mapToPojo((Map<String, Object>) map, type);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapToPojo(Map<String, Object> map, Class<T> type) {
        try {
            T obj = type.getDeclaredConstructor().newInstance();
            for (Field f : collectPojoFields(type)) {
                Object raw = map.get(f.getName());
                if (raw == null) continue;
                f.setAccessible(true);
                f.set(obj, coerce(raw, f.getType()));
            }
            return obj;
        } catch (Exception ignored) { return null; }
    }

    private static Object coerce(Object value, Class<?> type) {
        if (value == null) return null;
        if (type.isInstance(value)) return value;
        if (type == String.class)                                  return String.valueOf(value);
        if ((type == int.class     || type == Integer.class) && value instanceof Number n) return n.intValue();
        if ((type == long.class    || type == Long.class)    && value instanceof Number n) return n.longValue();
        if ((type == double.class  || type == Double.class)  && value instanceof Number n) return n.doubleValue();
        if ((type == float.class   || type == Float.class)   && value instanceof Number n) return n.floatValue();
        if ((type == boolean.class || type == Boolean.class) && value instanceof Boolean b) return b;
        if (value instanceof String s) {
            try {
                if (type == int.class     || type == Integer.class) return Integer.parseInt(s.trim());
                if (type == long.class    || type == Long.class)    return Long.parseLong(s.trim());
                if (type == double.class  || type == Double.class)  return Double.parseDouble(s.trim());
                if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return value;
    }

    private static List<Field> collectPojoFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) fields.add(f);
            cls = cls.getSuperclass();
        }
        return fields;
    }

    // ── Parser JSON interno ────────────────────────────────────────────────

    private static final class JsonParser {
        private final String src;
        private int pos;

        JsonParser(String src) { this.src = src; }

        Object parse() {
            skipWs();
            if (pos >= src.length()) return null;
            return switch (src.charAt(pos)) {
                case '{'  -> parseObject();
                case '['  -> parseArray();
                case '"'  -> parseString();
                case 't', 'f' -> parseBool();
                case 'n'  -> { pos += 4; yield null; }
                default   -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;  // '{'
            skipWs();
            while (pos < src.length() && src.charAt(pos) != '}') {
                skipWs();
                if (src.charAt(pos) == '"') {
                    String key = parseString();
                    skipWs();
                    if (pos < src.length() && src.charAt(pos) == ':') pos++;
                    skipWs();
                    map.put(key, parse());
                }
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') pos++;
                skipWs();
            }
            if (pos < src.length()) pos++;  // '}'
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++;  // '['
            skipWs();
            while (pos < src.length() && src.charAt(pos) != ']') {
                list.add(parse());
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') pos++;
                skipWs();
            }
            if (pos < src.length()) pos++;  // ']'
            return list;
        }

        private String parseString() {
            pos++;  // '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/'  -> sb.append('/');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        case 'b'  -> sb.append('\b');
                        case 'f'  -> sb.append('\f');
                        case 'u'  -> {
                            if (pos + 4 <= src.length()) {
                                try {
                                    sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                                    pos += 4;
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        default   -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private boolean parseBool() {
            if (src.startsWith("true", pos))  { pos += 4; return true; }
            pos += 5;  // "false"
            return false;
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
                pos++;
            }
            String num = src.substring(start, pos);
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                try { return Double.parseDouble(num); } catch (NumberFormatException ignored) {}
            }
            try { return Long.parseLong(num); }
            catch (NumberFormatException e) {
                try { return Double.parseDouble(num); } catch (NumberFormatException e2) { return 0; }
            }
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }

    // ── Serialización ─────────────────────────────────────────────────────

    public static String toJson(Object value) {
        if (value == null)                   return "null";
        if (value instanceof String s)       return quote(s);
        if (value instanceof Number)         return value.toString();
        if (value instanceof Boolean)        return value.toString();
        if (value instanceof DBRow row)      return rowToJson(row);
        if (value instanceof DBRowSet rs)    return rowSetToJson(rs);
        if (value instanceof List<?> list)   return listToJson(list);
        if (value instanceof Map<?, ?> map)  return mapToJson(map);
        return reflectToJson(value);
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private static String quote(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n",  "\\n")
                .replace("\r",  "\\r")
                .replace("\t",  "\\t")
                + "\"";
    }

    private static String rowToJson(DBRow row) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String k : row.keySet()) {
            if (!first) sb.append(',');
            sb.append(quote(k)).append(':').append(toJson(row.get(k)));
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String rowSetToJson(DBRowSet rs) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (DBRow row : rs.result()) {
            if (!first) sb.append(',');
            sb.append(rowToJson(row));
            first = false;
        }
        return sb.append(']').toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            sb.append(toJson(item));
            first = false;
        }
        return sb.append(']').toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String reflectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Method m : obj.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class) continue;
            String name = m.getName();
            String key  = null;
            if (name.startsWith("get") && name.length() > 3 && !name.equals("getClass")) {
                key = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                key = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            if (key == null) continue;
            try {
                if (!first) sb.append(',');
                sb.append(quote(key)).append(':').append(toJson(m.invoke(obj)));
                first = false;
            } catch (Exception ignored) {}
        }
        return sb.append('}').toString();
    }
}
