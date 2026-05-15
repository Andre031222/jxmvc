/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas de runtime del framework — equivalente ligero de Spring Actuator.
 * Expuesto en {@code /jx/metrics}.
 *
 * <pre>
 *   // Registro automático por MainLxServlet en cada petición
 *   JxMetrics.record("/api/users", "GET", 200, 42);   // path, verb, status, ms
 *
 *   // Consulta manual
 *   JxMetrics.Summary summary = JxMetrics.summary();
 *   summary.totalRequests();
 *   summary.totalErrors();
 *   summary.avgResponseMs();
 * </pre>
 */
public final class JxMetrics {

    private JxMetrics() {}

    // ── Contadores globales ───────────────────────────────────────────────

    private static final AtomicLong totalRequests   = new AtomicLong(0);
    private static final AtomicLong totalErrors     = new AtomicLong(0);
    private static final AtomicLong totalResponseMs = new AtomicLong(0);

    /** Contadores por ruta: "{verb}:{path}" → [requests, errors, totalMs] */
    private static final ConcurrentHashMap<String, long[]> perRoute = new ConcurrentHashMap<>();

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra una petición completada.
     *
     * @param path       ruta normalizada, ej. {@code "/api/users"}
     * @param verb       método HTTP, ej. {@code "GET"}
     * @param statusCode código de respuesta HTTP
     * @param durationMs tiempo de procesamiento en milisegundos
     */
    public static void record(String path, String verb, int statusCode, long durationMs) {
        totalRequests.incrementAndGet();
        totalResponseMs.addAndGet(durationMs);
        if (statusCode >= 400) totalErrors.incrementAndGet();

        String key = verb.toUpperCase() + ":" + (path == null ? "/" : path);
        long[] counters = perRoute.computeIfAbsent(key, k -> new long[3]);
        synchronized (counters) {
            counters[0]++;                          // requests
            counters[2] += durationMs;              // totalMs
            if (statusCode >= 400) counters[1]++;   // errors
        }
    }

    /** Retorna un snapshot de las métricas actuales. */
    public static Summary summary() {
        long req = totalRequests.get();
        long err = totalErrors.get();
        long ms  = totalResponseMs.get();
        long avg = req > 0 ? ms / req : 0;
        return new Summary(req, err, avg, Map.copyOf(perRoute));
    }

    /** Resetea todos los contadores. */
    public static void reset() {
        totalRequests.set(0);
        totalErrors.set(0);
        totalResponseMs.set(0);
        perRoute.clear();
    }

    // ── JSON ──────────────────────────────────────────────────────────────

    /** Serializa el resumen a JSON para el endpoint {@code /jx/metrics}. */
    public static String toJson() {
        Summary s = summary();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"totalRequests\":").append(s.totalRequests())
          .append(",\"totalErrors\":").append(s.totalErrors())
          .append(",\"avgResponseMs\":").append(s.avgResponseMs())
          .append(",\"routes\":{");

        boolean first = true;
        for (Map.Entry<String, long[]> e : s.routes().entrySet()) {
            if (!first) sb.append(',');
            long[] c = e.getValue();
            long routeAvg = c[0] > 0 ? c[2] / c[0] : 0;
            sb.append('"').append(e.getKey()).append("\":{")
              .append("\"requests\":").append(c[0])
              .append(",\"errors\":").append(c[1])
              .append(",\"avgMs\":").append(routeAvg)
              .append('}');
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }

    // ── Summary ───────────────────────────────────────────────────────────

    public record Summary(
            long totalRequests,
            long totalErrors,
            long avgResponseMs,
            Map<String, long[]> routes) {}
}
