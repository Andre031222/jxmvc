/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo
///  v3.0.0   : R. Andre Vilca Solorzano

package jxmvc.core;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase base para endpoints WebSocket de JxMVC.
 * Extiende esta clase y anótala con {@code @JxMapping.JxWsEndpoint}.
 *
 * <p>Ejemplo mínimo — chat de sala (solo necesitas {@code @JxWsEndpoint},
 * {@link JxWsRegistrar} registra el endpoint automáticamente al arrancar):
 * <pre>
 *   &#64;JxMapping.JxWsEndpoint("/ws/chat/{sala}")
 *   public class ChatEndpoint extends JxWebSocket {
 *
 *       &#64;Override
 *       protected void onOpen(Session session, String... pathVars) {
 *           String sala = pathVars.length > 0 ? pathVars[0] : "general";
 *           joinRoom(sala, session);
 *           broadcast(sala, "[" + session.getId() + "] conectado");
 *       }
 *
 *       &#64;Override
 *       protected void onMessage(Session session, String message) {
 *           String sala = (String) session.getUserProperties().get("sala");
 *           broadcast(sala, session.getId() + ": " + message);
 *       }
 *
 *       &#64;Override
 *       protected void onClose(Session session) {
 *           String sala = (String) session.getUserProperties().get("sala");
 *           leaveRoom(sala, session);
 *           broadcast(sala, "[" + session.getId() + "] desconectado");
 *       }
 *   }
 * </pre>
 */
public abstract class JxWebSocket {

    private static final JxLogger log = JxLogger.getLogger(JxWebSocket.class);

    // ── Salas — mapa sala → sesiones activas ─────────────────────────────
    private static final Map<String, Set<Session>> rooms = new ConcurrentHashMap<>();

    // ── Sesiones globales (todas las conexiones activas) ──────────────────
    private static final Set<Session> allSessions =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ── Hooks que la subclase implementa ─────────────────────────────────

    /**
     * Conexión abierta. {@code pathVars} contiene los segmentos de ruta variables
     * en el orden en que aparecen en el path del endpoint.
     */
    protected void onOpen(Session session, String... pathVars) {}

    /** Mensaje de texto recibido. */
    protected void onMessage(Session session, String message) {}

    /** Mensaje binario recibido. */
    protected void onBinary(Session session, byte[] data) {}

    /** Conexión cerrada. */
    protected void onClose(Session session) {}

    /** Error en la sesión. */
    protected void onError(Session session, Throwable error) {
        log.error("[WS] Error en sesión {}: {}", session.getId(), error.getMessage());
    }

    // ── Hooks Jakarta WebSocket — delegación interna ──────────────────────

    @OnOpen
    public final void _onOpen(Session session) {
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
        allSessions.remove(session);
        removeFromAllRooms(session);
        onClose(session);
    }

    @OnError
    public final void _onError(Session session, Throwable error) {
        onError(session, error);
    }

    // ── API de mensajería ─────────────────────────────────────────────────

    /** Envía un mensaje a una sesión específica. */
    protected void send(Session session, String message) {
        if (session != null && session.isOpen()) {
            try { session.getBasicRemote().sendText(message); }
            catch (IOException e) { log.warn("[WS] Error enviando a {}: {}", session.getId(), e.getMessage()); }
        }
    }

    /** Difunde a todos los integrantes de una sala. */
    protected void broadcast(String room, String message) {
        Set<Session> sessions = rooms.getOrDefault(room, Collections.emptySet());
        for (Session s : sessions) send(s, message);
    }

    /** Difunde a todas las sesiones abiertas (todas las salas). */
    protected void broadcastAll(String message) {
        for (Session s : allSessions) send(s, message);
    }

    // ── Gestión de salas ──────────────────────────────────────────────────

    /** Une una sesión a una sala. */
    protected void joinRoom(String room, Session session) {
        rooms.computeIfAbsent(room, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
             .add(session);
        session.getUserProperties().put("sala", room);
    }

    /** Retira una sesión de una sala. */
    protected void leaveRoom(String room, Session session) {
        Set<Session> s = rooms.get(room);
        if (s != null) { s.remove(session); if (s.isEmpty()) rooms.remove(room); }
    }

    /** Número de conexiones activas en una sala. */
    protected int roomSize(String room) {
        return rooms.getOrDefault(room, Collections.emptySet()).size();
    }

    /** Número total de conexiones WebSocket abiertas. */
    public static int totalConnections() { return allSessions.size(); }

    // ── Internos ──────────────────────────────────────────────────────────

    private void removeFromAllRooms(Session session) {
        rooms.forEach((room, sessions) -> sessions.remove(session));
        rooms.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private String[] extractPathVars(Session session) {
        Map<String, String> params = session.getPathParameters();
        return params == null ? new String[0] : params.values().toArray(new String[0]);
    }
}
