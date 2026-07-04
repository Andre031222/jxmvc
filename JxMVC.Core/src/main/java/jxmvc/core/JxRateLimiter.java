/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Limitador de tasa en memoria — motor para {@link JxMapping.JxRateLimit}.
 *
 * <p>Algoritmo de ventana deslizante por IP + clave de ruta.
 * Los contadores se limpian automáticamente al expirar la ventana.
 *
 * <pre>
 *   // Uso desde MainLxServlet (automático vía @JxRateLimit):
 *   if (!JxRateLimiter.allow(clientIp, "POST:/login", 10, 60)) {
 *       // 429 Too Many Requests
 *   }
 * </pre>
 */
public final class JxRateLimiter {

    private JxRateLimiter() {}

    /** Entrada: [count, windowStart, windowMs] */
    private static final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    private static final java.util.concurrent.ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jx-ratelimiter-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        // Daemon de limpieza: elimina solo buckets cuya propia ventana ya expiró
        CLEANER.scheduleWithFixedDelay(JxRateLimiter::cleanupExpired, 10, 10, TimeUnit.MINUTES);
    }

    /** Detiene el daemon de limpieza — llamar en el shutdown del contenedor. */
    public static void shutdown() {
        CLEANER.shutdownNow();
    }

    /**
     * Verifica y registra una petición.
     *
     * @param clientKey  identificador del cliente (ej. IP)
     * @param routeKey   clave de ruta (ej. {@code "POST:/api/login"})
     * @param maxReqs    número máximo de peticiones en la ventana
     * @param windowSecs duración de la ventana en segundos
     * @return {@code true} si la petición está permitida, {@code false} si supera el límite
     */
    public static boolean allow(String clientKey, String routeKey, int maxReqs, long windowSecs) {
        String key  = clientKey + "|" + routeKey;
        long   now  = System.currentTimeMillis();
        long   winMs = windowSecs * 1000;

        long[] bucket = buckets.computeIfAbsent(key, k -> new long[]{0, now, winMs});
        synchronized (bucket) {
            // Resetear si la ventana expiró
            if (now - bucket[1] >= winMs) {
                bucket[0] = 0;
                bucket[1] = now;
            }
            bucket[2] = winMs;
            if (bucket[0] >= maxReqs) return false;
            bucket[0]++;
            return true;
        }
    }

    /**
     * Retorna las peticiones restantes en la ventana actual para el cliente y ruta dados.
     *
     * @return peticiones restantes, o -1 si no hay registro
     */
    public static long remaining(String clientKey, String routeKey, int maxReqs, long windowSecs) {
        String key   = clientKey + "|" + routeKey;
        long   now   = System.currentTimeMillis();
        long   winMs = windowSecs * 1000;
        long[] bucket = buckets.get(key);
        if (bucket == null) return maxReqs;
        synchronized (bucket) {
            if (now - bucket[1] >= winMs) return maxReqs;
            return Math.max(0, maxReqs - bucket[0]);
        }
    }

    /** Limpia todos los buckets expirados. Llamar periódicamente para evitar memory leak. */
    public static void cleanup(long windowSecs) {
        long cutoff = System.currentTimeMillis() - windowSecs * 1000;
        buckets.entrySet().removeIf(e -> {
            long[] b = e.getValue();
            synchronized (b) { return b[1] < cutoff; }
        });
    }

    /** Elimina los buckets cuya propia ventana ya expiró (respeta ventanas largas activas). */
    static void cleanupExpired() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> {
            long[] b = e.getValue();
            synchronized (b) { return now - b[1] >= b[2]; }
        });
    }
}
