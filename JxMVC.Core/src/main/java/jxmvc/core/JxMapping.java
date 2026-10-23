package jxmvc.core;

import java.lang.annotation.*;

public final class JxMapping {
    private JxMapping() {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxControllerMapping {
        String value() default "";
        boolean strict() default false;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxControllerMain {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxGetMapping    { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxPostMapping   { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxPutMapping    { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxDeleteMapping { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxPatchMapping  { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxAnyMapping    { String[] value() default {""}; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface JxCors {
        String[] origins()         default {};
        String[] hosts()           default {};
        String[] methods()         default {};
        boolean allowCredentials() default false;
        long maxAge()              default 3600L;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface JxAuth {
        String[] roles()   default {};
        boolean required() default true;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface JxRequireAuth {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface JxRequireRole { String[] value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface JxCsrfExempt {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxPathVar {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxParam {
        String value() default "";
        boolean required() default false;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxBody {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxValid {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxService {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    public @interface JxInject {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxExceptionHandler {
        Class<? extends Throwable>[] value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxControllerAdvice {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface JxResponseStatus {
        int value();
        String reason() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxRequestHeader {
        String value();
        boolean required()     default false;
        String defaultValue()  default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface JxCookieValue {
        String value();
        boolean required()     default false;
        String defaultValue()  default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxScheduled {
        long fixedDelay()   default -1;
        long fixedRate()    default -1;
        long initialDelay() default 0;
        String cron() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface JxTransactional {
        boolean readOnly() default false;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxCacheable {
        String value();
        String key() default "";
        long ttl() default 300;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxCacheEvict {
        String value();
        String key() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    public @interface JxValue {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxEventListener {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxRestController {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    public @interface JxId {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    public @interface JxColumn {
        String value()       default "";
        boolean insertable() default true;
        boolean updatable()  default true;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxTable {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxBeforeAction {
        String[] only() default {};
        String[] except() default {};
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxAfterAction {
        String[] only() default {};
        String[] except() default {};
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxConfigProperties {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxQuery {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxSoftDelete {
        String column()       default "deleted";
        String deletedValue() default "1";
        String activeValue()  default "0";
    }

    enum ScopeType { SINGLETON, PROTOTYPE }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxScope {
        ScopeType value() default ScopeType.SINGLETON;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface JxDS {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxModelAttr {
        String value() default "";
        String[] only() default {};
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxRetry {
        int attempts()  default 3;
        long backoff()  default 500;
        Class<? extends Throwable>[] on() default {};
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface JxAsync {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface JxRateLimit {
        int requests()  default 100;
        long window()   default 60;
    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxCacheManager {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface JxWsEndpoint {
        String value();
        String[] subprotocols() default {};
    }
}
