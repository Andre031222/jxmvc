/// JxMVC Open-source project 2024 - 2026
/// -------------------------------------------
///  coded by : Dr. Ramiro Pedro Laura Murillo

package jxmvc.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de conexiones JDBC ligero — cero dependencias externas.
 * Reemplaza una conexión por instancia con un pool compartido y reutilizable.
 *
 * <pre>
 * # application.properties
 * jxmvc.pool.enabled=true
 * jxmvc.pool.size=10
 * jxmvc.pool.timeout=5
 * </pre>
 *
 * Si {@code jxmvc.pool.enabled=true}, {@link JxDB} lo usa automáticamente.
 * No es necesario configurarlo manualmente.
 */
public final class JxPool {

    private static volatile JxPool globalInstance;

    private final LinkedBlockingQueue<Connection> idle;
    private final AtomicInteger total = new AtomicInteger(0);
    private final int maxSize;
    private final int timeoutSecs;
    private final String url;
    private final String user;
    private final String pass;
    private final String engine;

    // ── Acceso global ─────────────────────────────────────────────────────

    /** Devuelve el pool global o {@code null} si no está configurado. */
    public static JxPool global() {
        if (globalInstance == null && BaseDbResolver.poolEnabled()) {
            synchronized (JxPool.class) {
                if (globalInstance == null) {
                    globalInstance = new JxPool(
                            BaseDbResolver.url(),
                            BaseDbResolver.user(),
                            BaseDbResolver.pass(),
                            BaseDbResolver.poolSize(),
                            BaseDbResolver.poolTimeout()
                    );
                }
            }
        }
        return globalInstance;
    }

    /** Reemplaza el pool global (útil en tests o multi-tenant). */
    public static void setGlobal(JxPool pool) { globalInstance = pool; }

    public static boolean isEnabled() { return BaseDbResolver.poolEnabled(); }

    // ── Constructor ───────────────────────────────────────────────────────

    public JxPool(String url, String user, String pass, int maxSize, int timeoutSecs) {
        this.url         = url;
        this.user        = user;
        this.pass        = pass;
        this.maxSize     = maxSize;
        this.timeoutSecs = timeoutSecs;
        this.engine      = detectEngine(url);
        this.idle        = new LinkedBlockingQueue<>(maxSize);
        prewarm(Math.min(2, maxSize));
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Obtiene una conexión del pool.
     * Espera hasta {@code timeoutSecs} segundos.
     *
     * @throws SQLException si no hay conexiones disponibles en el tiempo límite
     */
    public Connection borrow() throws SQLException {
        Connection conn = idle.poll();
        if (conn != null && isValid(conn)) return conn;

        if (total.get() < maxSize) {
            total.incrementAndGet();
            return open();
        }

        // Esperar a que se libere una conexión
        try {
            conn = idle.poll(timeoutSecs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Pool interrumpido esperando conexión");
        }
        if (conn == null) throw new SQLException("Pool agotado: no hay conexiones disponibles en " + timeoutSecs + "s");
        if (!isValid(conn)) { total.decrementAndGet(); return open(); }
        return conn;
    }

    /**
     * Devuelve una conexión al pool.
     * Llamar siempre después de usar — idealmente con try-with-resources via {@link PooledConnection}.
     */
    public void release(Connection conn) {
        if (conn == null) return;
        if (!isValid(conn)) { total.decrementAndGet(); return; }
        idle.offer(conn);
    }

    public int available() { return idle.size(); }
    public int total()     { return total.get(); }
    public int maxSize()   { return maxSize; }
    public int active()    { return total.get() - idle.size(); }

    /**
     * Retorna estadísticas del pool como {@link DBRow} serializable a JSON.
     *
     * <pre>
     *   return Json(GenApi.JsonRow(JxPool.global().stats()));
     *   // → {"total":5,"idle":3,"active":2,"max":10,"engine":"PSSQL"}
     * </pre>
     */
    public DBRow stats() {
        return DBRow.of(
            "total",   total.get(),
            "idle",    idle.size(),
            "active",  active(),
            "max",     maxSize,
            "timeout", timeoutSecs,
            "engine",  engine
        );
    }

    /**
     * Valida las conexiones idle y descarta las que el servidor BD cerró por inactividad.
     * Llamar periódicamente (cada 3-5 minutos) para evitar errores en la primera petición
     * tras un periodo de inactividad prolongado.
     *
     * <p>Invocado automáticamente por el scheduler de {@code MainLxServlet}.
     */
    public void keepAlive() {
        int size = idle.size();
        for (int i = 0; i < size; i++) {
            Connection conn = idle.poll();
            if (conn == null) break;
            if (isValid(conn)) {
                idle.offer(conn);  // sana: devolverla al pool
            } else {
                total.decrementAndGet();  // muerta: descartarla
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /** Cierra todas las conexiones del pool. */
    public void shutdown() {
        Connection conn;
        while ((conn = idle.poll()) != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        total.set(0);
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private void prewarm(int count) {
        for (int i = 0; i < count; i++) {
            try {
                idle.offer(open());
                total.incrementAndGet();
            } catch (SQLException ignored) {}
        }
    }

    private Connection open() throws SQLException {
        loadDriver();
        return switch (engine) {
            case "MSSQL" -> DriverManager.getConnection(url);
            default      -> DriverManager.getConnection(url, user, pass);
        };
    }

    private void loadDriver() throws SQLException {
        String cls = switch (engine) {
            case "PSSQL" -> "org.postgresql.Driver";
            case "MySQL" -> "com.mysql.cj.jdbc.Driver";
            case "MSSQL" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default -> throw new SQLException("Motor no reconocido: " + url);
        };
        try { Class.forName(cls); }
        catch (ClassNotFoundException e) { throw new SQLException("Driver no encontrado: " + cls, e); }
    }

    private boolean isValid(Connection conn) {
        try { return conn != null && !conn.isClosed() && conn.isValid(1); }
        catch (SQLException e) { return false; }
    }

    private String detectEngine(String u) {
        if (u == null) return "Unknown";
        String l = u.toLowerCase();
        if (l.contains(":postgresql:")) return "PSSQL";
        if (l.contains(":sqlserver:"))  return "MSSQL";
        if (l.contains(":mysql:"))      return "MySQL";
        return "Unknown";
    }
}
