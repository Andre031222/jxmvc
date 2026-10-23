package jxmvc.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class JxMetrics {
    private JxMetrics() {}

    private static final AtomicLong totalRequests   = new AtomicLong(0);
    private static final AtomicLong totalErrors     = new AtomicLong(0);
    private static final AtomicLong totalResponseMs = new AtomicLong(0);

    private static final AtomicLong[] statusClasses = {
            new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong(), new AtomicLong()};

    private static final ConcurrentHashMap<String, long[]> perRoute = new ConcurrentHashMap<>();

    private static final int MAX_ROUTES = 500;

    public static void record(String path, String verb, int statusCode, long durationMs) {
        totalRequests.incrementAndGet();
        totalResponseMs.addAndGet(durationMs);
        if (statusCode >= 400) totalErrors.incrementAndGet();
        int cls = statusCode / 100;
        if (cls >= 1 && cls <= 5) statusClasses[cls - 1].incrementAndGet();

        String key = verb.toUpperCase() + ":" + (path == null ? "/" : path);
        long[] counters = perRoute.get(key);
        if (counters == null) {
            if (perRoute.size() >= MAX_ROUTES) key = verb.toUpperCase() + ":__other__";
            counters = perRoute.computeIfAbsent(key, k -> new long[]{0, 0, 0, Long.MAX_VALUE, 0});
        }
        synchronized (counters) {
            counters[0]++;
            counters[2] += durationMs;
            if (statusCode >= 400) counters[1]++;
            if (durationMs < counters[3]) counters[3] = durationMs;
            if (durationMs > counters[4]) counters[4] = durationMs;
        }
    }

    public static Summary summary() {
        long req = totalRequests.get();
        long err = totalErrors.get();
        long ms  = totalResponseMs.get();
        long avg = req > 0 ? ms / req : 0;
        return new Summary(req, err, avg, Map.copyOf(perRoute));
    }

    public static void reset() {
        totalRequests.set(0);
        totalErrors.set(0);
        totalResponseMs.set(0);
        for (AtomicLong c : statusClasses) c.set(0);
        perRoute.clear();
    }

    public static String toJson() {
        Summary s = summary();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"totalRequests\":").append(s.totalRequests())
          .append(",\"totalErrors\":").append(s.totalErrors())
          .append(",\"avgResponseMs\":").append(s.avgResponseMs())
          .append(",\"status\":{");
        for (int i = 0; i < statusClasses.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(i + 1).append("xx\":").append(statusClasses[i].get());
        }
        sb.append("},\"routes\":{");

        boolean first = true;
        for (Map.Entry<String, long[]> e : s.routes().entrySet()) {
            if (!first) sb.append(',');
            long[] c = e.getValue();
            long req, err, avg, min, max;
            synchronized (c) {
                req = c[0]; err = c[1]; avg = req > 0 ? c[2] / req : 0;
                min = req > 0 ? c[3] : 0; max = c[4];
            }
            sb.append(JxJson.quote(e.getKey())).append(":{")
              .append("\"requests\":").append(req)
              .append(",\"errors\":").append(err)
              .append(",\"avgMs\":").append(avg)
              .append(",\"minMs\":").append(min)
              .append(",\"maxMs\":").append(max)
              .append('}');
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }

    public record Summary(
            long totalRequests,
            long totalErrors,
            long avgResponseMs,
            Map<String, long[]> routes) {}
}
