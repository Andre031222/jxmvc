package jxmvc.core;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JxWebSocket {
    private static final JxLogger log = JxLogger.getLogger(JxWebSocket.class);

    private static final int MAX_CONNECTIONS =
            BaseDbResolver.propertyInt("jxmvc.ws.maxConnections", 0);

    private static final java.util.concurrent.atomic.AtomicInteger connectionCount =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private static final Map<String, Set<Session>> rooms = new ConcurrentHashMap<>();

    private static final Set<Session> allSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected void onOpen(Session session, String... pathVars) {}

    protected void onMessage(Session session, String message) {}

    protected void onBinary(Session session, byte[] data) {}

    protected void onClose(Session session) {}

    protected void onError(Session session, Throwable error) {
        log.error("[WS] Error en sesión {}: {}", session.getId(), error.getMessage());
    }

    @OnOpen
    public final void _onOpen(Session session) {
        if (MAX_CONNECTIONS > 0 && connectionCount.incrementAndGet() > MAX_CONNECTIONS) {
            connectionCount.decrementAndGet();
            log.warn("[WS] Conexión rechazada: límite de {} alcanzado", MAX_CONNECTIONS);
            try { session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Límite de conexiones")); }
            catch (IOException ignored) {}
            return;
        }
        allSessions.add(session);
        String[] vars = extractPathVars(session);
        log.debug("[WS] Abierta sesión {} — {}", session.getId(), session.getRequestURI());
        onOpen(session, vars);
    }

    @OnMessage
    public final void _onMessage(Session session, String message) {
        onMessage(session, message);
    }

    @OnMessage
    public final void _onBinary(Session session, byte[] data) {
        onBinary(session, data);
    }

    @OnClose
    public final void _onClose(Session session) {
        if (allSessions.remove(session) && MAX_CONNECTIONS > 0) connectionCount.decrementAndGet();
        removeFromAllRooms(session);
        onClose(session);
    }

    @OnError
    public final void _onError(Session session, Throwable error) {
        onError(session, error);
        if (session != null && !session.isOpen()) {
            if (allSessions.remove(session) && MAX_CONNECTIONS > 0) connectionCount.decrementAndGet();
            removeFromAllRooms(session);
        }
    }

    protected void send(Session session, String message) {
        if (session == null || !session.isOpen()) return;
        synchronized (session) {
            try { session.getBasicRemote().sendText(message); }
            catch (IOException e) { log.warn("[WS] Error enviando a {}: {}", session.getId(), e.getMessage()); }
        }
    }

    protected void broadcast(String room, String message) {
        Set<Session> sessions = rooms.getOrDefault(room, Collections.emptySet());
        for (Session s : sessions) send(s, message);
    }

    protected void broadcastAll(String message) {
        for (Session s : allSessions) send(s, message);
    }

    protected void joinRoom(String room, Session session) {
        rooms.compute(room, (k, set) -> {
            if (set == null) set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            set.add(session);
            return set;
        });
        session.getUserProperties().put("sala", room);
    }

    protected void leaveRoom(String room, Session session) {
        rooms.compute(room, (k, set) -> {
            if (set == null) return null;
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
    }

    protected int roomSize(String room) {
        return rooms.getOrDefault(room, Collections.emptySet()).size();
    }

    public static int totalConnections() { return allSessions.size(); }

    private void removeFromAllRooms(Session session) {
        for (String room : rooms.keySet()) {
            rooms.compute(room, (k, set) -> {
                if (set == null) return null;
                set.remove(session);
                return set.isEmpty() ? null : set;
            });
        }
    }

    private String[] extractPathVars(Session session) {
        Map<String, String> params = session.getPathParameters();
        return params == null ? new String[0] : params.values().toArray(new String[0]);
    }
}
