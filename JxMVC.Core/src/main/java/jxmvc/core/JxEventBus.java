package jxmvc.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JxEventBus {
    private static final JxLogger log = JxLogger.getLogger(JxEventBus.class);

    private static final ConcurrentHashMap<Class<?>, List<Listener>> LISTENERS =
            new ConcurrentHashMap<>();

    private static final java.util.Set<String> REGISTERED = ConcurrentHashMap.newKeySet();

    private static volatile boolean scanned = false;

    private JxEventBus() {}

    public static void publish(Object event) {
        ensureScanned();
        Class<?> type = event.getClass();
        LISTENERS.forEach((eventType, list) -> {
            if (eventType.isAssignableFrom(type)) {
                for (Listener l : list) {
                    try { l.invoke(event); }
                    catch (Exception e) { log.warn("listener error para {}: {}", type.getSimpleName(), e.getMessage()); }
                }
            }
        });
    }

    public static <T> void on(Class<T> eventType, java.util.function.Consumer<T> handler) {
        LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(event -> handler.accept(eventType.cast(event)));
    }

    public static void off(Class<?> eventType) {
        LISTENERS.remove(eventType);
    }

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
            String sig = System.identityHashCode(target) + "#"
                    + target.getClass().getName() + "#" + m.getName() + "#" + eventType.getName();
            if (!REGISTERED.add(sig)) continue;
            m.setAccessible(true);
            LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                     .add(event -> m.invoke(target, event));
        }
    }

    @FunctionalInterface
    private interface Listener {
        void invoke(Object event) throws Exception;
    }
}
