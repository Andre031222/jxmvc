package jxmvc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public final class JxTransaction {
    static final ThreadLocal<Connection> TX = new ThreadLocal<>();

    private JxTransaction() {}

    public static Connection current() { return TX.get(); }

    public static boolean isActive() { return TX.get() != null; }

    public static void run(Runnable work) {
        call(() -> { work.run(); return null; });
    }

    public static <T> T call(Callable<T> work) {
        boolean root = TX.get() == null;
        if (!root) {
            try { return work.call(); }
            catch (Exception e) { rethrow(e); return null; }
        }

        try (JxDB db = new JxDB()) {
            Connection conn = db.rawConnection();
            TX.set(conn);
            try {
                conn.setAutoCommit(false);
                T result = work.call();
                conn.commit();
                return result;
            } catch (Exception e) {
                safeRollback(conn);
                rethrow(e);
                return null;
            } finally {
                safeAutoCommit(conn);
                TX.remove();
            }
        } catch (Exception e) {
            rethrow(e);
            return null;
        }
    }

    private static void safeRollback(Connection conn) {
        try { conn.rollback(); } catch (SQLException ignored) {}
    }

    private static void safeAutoCommit(Connection conn) {
        try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
    }

    private static void rethrow(Exception e) {
        if (e instanceof RuntimeException r) throw r;
        throw new JxException(500, "Transaction error: " + e.getMessage());
    }
}
