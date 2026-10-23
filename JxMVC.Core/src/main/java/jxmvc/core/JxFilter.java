package jxmvc.core;

@FunctionalInterface
public interface JxFilter {
    boolean before(JxFilterContext ctx);

    default void after(JxFilterContext ctx) {}
}
