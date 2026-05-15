/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Motor de tareas programadas — sin dependencias externas.
 * Detecta métodos {@code @JxScheduled} en servicios {@code @JxService}
 * y los ejecuta con {@link ScheduledExecutorService}.
 *
 * <pre>
 *   &#64;JxService
 *   public class CleanupService {
 *
 *       &#64;JxScheduled(fixedDelay = 60_000, initialDelay = 5_000)
 *       public void purgeExpiredTokens() { ... }
 *
 *       &#64;JxScheduled(fixedRate = 30_000)
 *       public void refreshCache() { ... }
 *   }
 * </pre>
 *
 * Se inicia automáticamente en {@code MainLxServlet.init()}.
 * También se puede invocar manualmente:
 * <pre>
 *   JxScheduler.start();         // escanea servicios registrados
 *   JxScheduler.shutdown();      // libera el executor (en destroy)
 * </pre>
 */
public final class JxScheduler {

    private static final AtomicInteger threadId = new AtomicInteger(1);
    private static final JxLogger      log      = JxLogger.getLogger(JxScheduler.class);

    private static final ScheduledExecutorService exec =
            Executors.newScheduledThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors()),
                    r -> {
                        Thread t = new Thread(r, "jx-scheduler-" + threadId.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
            );

    private JxScheduler() {}

    // ── Inicio ────────────────────────────────────────────────────────────

    /**
     * Escanea todos los singletons de {@link JxServiceRegistry} y programa
     * los métodos anotados con {@code @JxScheduled}.
     * Llamar una sola vez desde {@code MainLxServlet.init()}.
     */
    public static void start() {
        for (Object service : JxServiceRegistry.values()) {
            scanAndSchedule(service);
        }
    }

    /**
     * Programa explícitamente un método con sus parámetros de scheduling.
     * Útil cuando el servicio no está en el registro automático.
     */
    public static void schedule(Object target, Method method, JxMapping.JxScheduled ann) {
        method.setAccessible(true);
        Runnable task = () -> {
            try { method.invoke(target); }
            catch (Exception e) {
                // FIX: tareas programadas ya no son silenciosas
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warn("@JxScheduled {}.{} falló: {}",
                        target.getClass().getSimpleName(), method.getName(), cause.getMessage());
            }
        };
        long initial = ann.initialDelay();
        if (ann.fixedDelay() > 0) {
            exec.scheduleWithFixedDelay(task, initial, ann.fixedDelay(), TimeUnit.MILLISECONDS);
        } else if (ann.fixedRate() > 0) {
            exec.scheduleAtFixedRate(task, initial, ann.fixedRate(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Programa una tarea con tasa fija sin necesidad de un servicio.
     * Útil para tareas de mantenimiento internas.
     *
     * @param task       tarea a ejecutar
     * @param initialMs  milisegundos antes del primer disparo
     * @param periodMs   milisegundos entre disparos
     */
    public static void scheduleAtFixedRate(Runnable task, long initialMs, long periodMs) {
        exec.scheduleAtFixedRate(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("scheduleAtFixedRate tarea falló: {}", e.getMessage()); }
        }, initialMs, periodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Programa una tarea con retardo fijo.
     *
     * @param task       tarea a ejecutar
     * @param initialMs  milisegundos antes del primer disparo
     * @param delayMs    milisegundos entre el fin de una ejecución y el inicio de la siguiente
     */
    public static void scheduleWithFixedDelay(Runnable task, long initialMs, long delayMs) {
        exec.scheduleWithFixedDelay(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("scheduleWithFixedDelay tarea falló: {}", e.getMessage()); }
        }, initialMs, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Ejecuta una tarea una sola vez después del retraso dado.
     *
     * <pre>
     *   JxScheduler.runOnce(() -> notificarAdmin(), 5_000); // en 5 segundos
     * </pre>
     */
    public static void runOnce(Runnable task, long delayMs) {
        exec.schedule(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("runOnce tarea falló: {}", e.getMessage()); }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Ejecuta una tarea inmediatamente en el pool del scheduler.
     */
    public static void runAsync(Runnable task) {
        exec.submit(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("runAsync tarea falló: {}", e.getMessage()); }
        });
    }

    /** Detiene el executor. Llamar desde {@code MainLxServlet.destroy()}. */
    public static void shutdown() {
        exec.shutdownNow();
    }

    /** Retorna {@code true} si el scheduler está activo. */
    public static boolean isRunning() {
        return !exec.isShutdown();
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private static void scanAndSchedule(Object target) {
        for (Method m : target.getClass().getMethods()) {
            JxMapping.JxScheduled ann = m.getAnnotation(JxMapping.JxScheduled.class);
            if (ann == null) continue;
            if (ann.fixedDelay() <= 0 && ann.fixedRate() <= 0) continue;
            schedule(target, m, ann);
        }
    }
}
