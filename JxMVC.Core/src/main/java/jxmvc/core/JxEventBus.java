/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bus de eventos en-proceso — cero dependencias externas.
 *
 * <p>Los listeners se descubren automáticamente en los singletons
 * de {@link JxServiceRegistry} anotados con {@code @JxEventListener}.
 * También se pueden registrar manualmente.
 *
 * <pre>
 *   // ── Definir un evento ────────────────────────────────────────────────
 *   public record UserCreatedEvent(long userId, String email) {}
 *
 *   // ── Publicar ─────────────────────────────────────────────────────────
 *   JxEventBus.publish(new UserCreatedEvent(id, email));
 *
 *   // ── Escuchar (en un @JxService) ───────────────────────────────────────
 *   &#64;JxService
 *   public class NotificationService {
 *
 *       &#64;JxEventListener
 *       public void onUserCreated(UserCreatedEvent e) {
 *           sendWelcomeEmail(e.email());
 *       }
 *   }
 *
 *   // ── Registro manual ───────────────────────────────────────────────────
 *   JxEventBus.on(MyEvent.class, event -> log.info("Event: " + event));
 * </pre>
 *
 * Los listeners se ejecutan <b>sincrónicamente</b> en el hilo del publicador.
 * Los errores en listeners se silencian para no interrumpir al publicador.
 */
public final class JxEventBus {

    /** Mapa: tipo de evento → lista de listeners. */
    private static final ConcurrentHashMap<Class<?>, List<Listener>> LISTENERS =
            new ConcurrentHashMap<>();

    private static volatile boolean scanned = false;

    private JxEventBus() {}

    // ── Publicación ───────────────────────────────────────────────────────

    /**
     * Publica un evento a todos los listeners registrados que aceptan
     * el tipo del evento o un supertipo.
     */
    public static void publish(Object event) {
        ensureScanned();
        Class<?> type = event.getClass();
        LISTENERS.forEach((eventType, list) -> {
            if (eventType.isAssignableFrom(type)) {
                for (Listener l : list) {
                    try { l.invoke(event); }
                    catch (Exception ignored) {}  // errores en listeners no interrumpen
                }
            }
        });
    }

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un listener funcional para un tipo de evento.
     *
     * <pre>
     *   JxEventBus.on(OrderEvent.class, e -> shippingService.schedule(e.orderId()));
     * </pre>
     */
    public static <T> void on(Class<T> eventType, java.util.function.Consumer<T> handler) {
        LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(event -> handler.accept(eventType.cast(event)));
    }

    /** Elimina todos los listeners de un tipo de evento. */
    public static void off(Class<?> eventType) {
        LISTENERS.remove(eventType);
    }

    // ── Escaneo automático ────────────────────────────────────────────────

    private static void ensureScanned() {
        if (scanned) return;
        synchronized (JxEventBus.class) {
            if (scanned) return;
            scan();
            scanned = true;
        }
    }

    private static void scan() {
        for (Object service : JxServiceRegistry.values()) {
            registerListeners(service);
        }
    }

    static void registerListeners(Object target) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getAnnotation(JxMapping.JxEventListener.class) == null) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> eventType = m.getParameterTypes()[0];
            m.setAccessible(true);
            LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                     .add(event -> m.invoke(target, event));
        }
    }

    // ── Listener interno ──────────────────────────────────────────────────

    @FunctionalInterface
    private interface Listener {
        void invoke(Object event) throws Exception;
    }
}
