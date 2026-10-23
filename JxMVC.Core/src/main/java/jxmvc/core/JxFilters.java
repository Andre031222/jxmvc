package jxmvc.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import jxmvc.core.JxLogger;

public final class JxFilters {
    private static final JxLogger      log     = JxLogger.getLogger(JxFilters.class);
    private static final List<JxFilter> filters = new CopyOnWriteArrayList<>();

    private JxFilters() {}

    public static void add(JxFilter filter) { filters.add(filter); }

    public static void add(int index, JxFilter filter) { filters.add(index, filter); }

    public static void remove(JxFilter filter) { filters.remove(filter); }

    public static void clear() { filters.clear(); }

    public static int count() { return filters.size(); }

    static boolean runBefore(JxFilterContext ctx) {
        for (JxFilter f : filters) {
            if (!f.before(ctx)) return false;
        }
        return true;
    }

    static void runAfter(JxFilterContext ctx) {
        for (int i = filters.size() - 1; i >= 0; i--) {
            try { filters.get(i).after(ctx); }
            catch (Exception e) {
                log.warn("JxFilter.after() falló: {}", e.getMessage());
            }
        }
    }
}
