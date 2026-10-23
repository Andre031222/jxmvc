package jxmvc.core;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class JxValidation {
    private JxValidation() {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxRequired { String message() default "es requerido"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxNotNull { String message() default "no puede ser nulo"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxMinLength { int value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxMaxLength { int value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxLength { int value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxMin { double value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxMax { double value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxRange {
        double min() default Double.MIN_VALUE;
        double max() default Double.MAX_VALUE;
        String message() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxEmail { String message() default "formato de email inválido"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxPhone { String message() default "formato de teléfono inválido"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxPattern { String value(); String message() default "formato inválido"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxPositive { String message() default "debe ser mayor que 0"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxNotEmpty { String message() default "no puede estar vacío"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxIn {
        String[] value();
        String message() default "valor no permitido";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxDigits { int value(); String message() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxSafe { String message() default "contiene caracteres no permitidos"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxFuture { String message() default "debe ser una fecha futura"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxPast { String message() default "debe ser una fecha pasada"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxUrl { String message() default "formato de URL inválido"; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface JxCheck {
        Class<? extends JxConstraint<?>> value();
        String message() default "";
    }

    public interface JxConstraint<T> {
        boolean isValid(T value);
        default String message() { return "valor inválido"; }
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+\\-().\\s\\d]{6,20}$");

    private static final Pattern SAFE_PATTERN =
            Pattern.compile("[;'\"\\\\<>]");

    private static final Pattern DIGITS_ONLY =
            Pattern.compile("^\\d+$");

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$");

    private static final ConcurrentHashMap<String, Pattern>    PATTERN_CACHE    = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, JxConstraint<?>> CONSTRAINT_CACHE = new ConcurrentHashMap<>();

    private static Pattern cachedPattern(String regex) {
        return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
    }

    @SuppressWarnings("unchecked")
    private static String runCustomConstraint(JxCheck check, Object value) {
        try {
            JxConstraint<Object> c = (JxConstraint<Object>)
                    CONSTRAINT_CACHE.computeIfAbsent(
                            check.value(),
                            cls -> {
                                try { return (JxConstraint<?>) cls.getDeclaredConstructor().newInstance(); }
                                catch (Exception e) { throw new RuntimeException(e); }
                            }
                    );
            if (!c.isValid(value)) {
                return check.message().isBlank() ? c.message() : check.message();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void validate(Object obj) {
        Map<String, String> errors = checkMap(obj);
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("{\"errors\":{");
            boolean first = true;
            for (Map.Entry<String, String> e : errors.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(e.getKey()).append("\":\"")
                  .append(e.getValue().replace("\"", "\\\"")).append('"');
                first = false;
            }
            sb.append("}}");
            throw new JxException(422, sb.toString());
        }
    }

    public static List<String> check(Object obj) {
        Map<String, String> map = checkMap(obj);
        List<String> list = new ArrayList<>();
        map.forEach((k, v) -> list.add(k + ": " + v));
        return list;
    }

    public static Map<String, String> checkMap(Object obj) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (obj == null) { errors.put("object", "no puede ser nulo"); return errors; }

        for (Field field : collectFields(obj.getClass())) {
            field.setAccessible(true);
            Object value;
            try { value = field.get(obj); }
            catch (IllegalAccessException e) { continue; }

            String name = field.getName();
            String error = validateField(field, value);
            if (error != null) errors.put(name, error);
        }
        return errors;
    }

    private static String validateField(Field field, Object value) {
        JxNotNull notNull = field.getAnnotation(JxNotNull.class);
        if (notNull != null && value == null) return notNull.message();

        JxRequired req = field.getAnnotation(JxRequired.class);
        if (req != null && isEmpty(value)) return req.message();

        JxNotEmpty notEmpty = field.getAnnotation(JxNotEmpty.class);
        if (notEmpty != null && (value == null || String.valueOf(value).isBlank()))
            return notEmpty.message();

        if (isEmpty(value)) return null;

        String str = String.valueOf(value).trim();

        JxLength length = field.getAnnotation(JxLength.class);
        if (length != null && str.length() != length.value())
            return "longitud exacta requerida: " + length.value() + " caracteres"
                 + (length.message().isBlank() ? "" : " — " + length.message());

        JxMinLength minLen = field.getAnnotation(JxMinLength.class);
        if (minLen != null && str.length() < minLen.value())
            return "mínimo " + minLen.value() + " caracteres"
                 + (minLen.message().isBlank() ? "" : " — " + minLen.message());

        JxMaxLength maxLen = field.getAnnotation(JxMaxLength.class);
        if (maxLen != null && str.length() > maxLen.value())
            return "máximo " + maxLen.value() + " caracteres"
                 + (maxLen.message().isBlank() ? "" : " — " + maxLen.message());

        JxEmail email = field.getAnnotation(JxEmail.class);
        if (email != null && !EMAIL_PATTERN.matcher(str).matches()) return email.message();

        JxPhone phone = field.getAnnotation(JxPhone.class);
        if (phone != null && !PHONE_PATTERN.matcher(str).matches()) return phone.message();

        JxPattern pat = field.getAnnotation(JxPattern.class);
        if (pat != null && !cachedPattern(pat.value()).matcher(str).matches()) return pat.message();

        JxIn in = field.getAnnotation(JxIn.class);
        if (in != null) {
            boolean found = false;
            for (String allowed : in.value()) { if (allowed.equals(str)) { found = true; break; } }
            if (!found) return in.message() + " — permitidos: " + String.join(", ", in.value());
        }

        JxSafe safe = field.getAnnotation(JxSafe.class);
        if (safe != null && SAFE_PATTERN.matcher(str).find()) return safe.message();

        JxUrl url = field.getAnnotation(JxUrl.class);
        if (url != null && !URL_PATTERN.matcher(str).matches()) return url.message();

        JxDigits digits = field.getAnnotation(JxDigits.class);
        if (digits != null) {
            if (!DIGITS_ONLY.matcher(str).matches())
                return "debe contener solo dígitos" + (digits.message().isBlank() ? "" : " — " + digits.message());
            if (str.length() != digits.value())
                return "debe tener exactamente " + digits.value() + " dígitos"
                     + (digits.message().isBlank() ? "" : " — " + digits.message());
        }

        JxFuture future = field.getAnnotation(JxFuture.class);
        if (future != null) {
            if (value instanceof LocalDate d      && !d.isAfter(LocalDate.now()))       return future.message();
            if (value instanceof LocalDateTime dt && !dt.isAfter(LocalDateTime.now()))  return future.message();
        }

        JxPast past = field.getAnnotation(JxPast.class);
        if (past != null) {
            if (value instanceof LocalDate d      && !d.isBefore(LocalDate.now()))      return past.message();
            if (value instanceof LocalDateTime dt && !dt.isBefore(LocalDateTime.now())) return past.message();
        }

        JxCheck check = field.getAnnotation(JxCheck.class);
        if (check != null) {
            String err = runCustomConstraint(check, value);
            if (err != null) return err;
        }

        if (value instanceof Number num) {
            double d = num.doubleValue();
            String err = checkNumeric(field, d);
            if (err != null) return err;
        } else {
            JxMin min = field.getAnnotation(JxMin.class);
            JxMax max = field.getAnnotation(JxMax.class);
            JxRange range = field.getAnnotation(JxRange.class);
            JxPositive pos = field.getAnnotation(JxPositive.class);
            if (min != null || max != null || range != null || pos != null) {
                try {
                    double d = Double.parseDouble(str);
                    String err = checkNumeric(field, d);
                    if (err != null) return err;
                } catch (NumberFormatException e) {
                    return "debe ser un número";
                }
            }
        }

        return null;
    }

    private static String checkNumeric(Field field, double d) {
        JxMin min = field.getAnnotation(JxMin.class);
        if (min != null && d < min.value())
            return "mínimo " + min.value() + (min.message().isBlank() ? "" : " — " + min.message());

        JxMax max = field.getAnnotation(JxMax.class);
        if (max != null && d > max.value())
            return "máximo " + max.value() + (max.message().isBlank() ? "" : " — " + max.message());

        JxRange range = field.getAnnotation(JxRange.class);
        if (range != null && (d < range.min() || d > range.max()))
            return "debe estar entre " + range.min() + " y " + range.max()
                 + (range.message().isBlank() ? "" : " — " + range.message());

        JxPositive pos = field.getAnnotation(JxPositive.class);
        if (pos != null && d <= 0) return pos.message();

        return null;
    }

    private static List<Field> collectFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) fields.add(f);
            cls = cls.getSuperclass();
        }
        return fields;
    }

    private static boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isBlank();
        return false;
    }
}
