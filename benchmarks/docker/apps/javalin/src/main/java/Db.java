import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.jdbcx.JdbcConnectionPool;

public final class Db {
    private static final int ROWS = 1000;
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static volatile JdbcConnectionPool pool;

    private Db() {}

    private static synchronized void init() {
        if (pool != null) return;
        JdbcConnectionPool p = JdbcConnectionPool.create(
            "jdbc:h2:mem:bench;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = p.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE note(id INT PRIMARY KEY, title VARCHAR(64), body VARCHAR(256))");
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO note(id,title,body) VALUES(?,?,?)")) {
                for (int i = 0; i < ROWS; i++) {
                    ins.setInt(1, i);
                    ins.setString(2, "Note " + i);
                    ins.setString(3, "Body of note " + i + " for the database benchmark endpoint.");
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
        pool = p;
    }

    public static String json() {
        if (pool == null) init();
        int id = Math.floorMod(SEQ.getAndIncrement(), ROWS);
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,title,body FROM note WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return "{\"id\":" + rs.getInt(1)
                     + ",\"title\":\"" + rs.getString(2)
                     + "\",\"body\":\"" + rs.getString(3) + "\"}";
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
