package jxmvc.core;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class JxLogger {
    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static volatile Level GLOBAL_LEVEL = resolveGlobalLevel();

    public static void setLevel(Level level) { GLOBAL_LEVEL = level; }
    public static Level getLevel()           { return GLOBAL_LEVEL; }

    private static Level resolveGlobalLevel() {
        String s = BaseDbResolver.property("jxmvc.log.level", "INFO");
        try { return Level.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return Level.INFO; }
    }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String name;

    private JxLogger(String name) { this.name = name; }

    public static JxLogger getLogger(Class<?> cls)  { return new JxLogger(cls.getSimpleName()); }
    public static JxLogger getLogger(String name)   { return new JxLogger(name); }

    public void debug(String msg, Object... args) { log(Level.DEBUG, msg, null, args); }
    public void info (String msg, Object... args) { log(Level.INFO,  msg, null, args); }
    public void warn (String msg, Object... args) { log(Level.WARN,  msg, null, args); }
    public void error(String msg, Object... args) { log(Level.ERROR, msg, null, args); }

    public void error(String msg, Throwable t)    { log(Level.ERROR, msg, t); }
    public void warn (String msg, Throwable t)    { log(Level.WARN,  msg, t); }

    public boolean isDebugEnabled() { return Level.DEBUG.ordinal() >= GLOBAL_LEVEL.ordinal(); }
    public boolean isInfoEnabled()  { return Level.INFO.ordinal()  >= GLOBAL_LEVEL.ordinal(); }

    private void log(Level level, String msg, Throwable t, Object... args) {
        if (level.ordinal() < GLOBAL_LEVEL.ordinal()) return;

        String ts     = LocalDateTime.now().format(FMT);
        String thread = Thread.currentThread().getName();
        String body   = format(msg, args);
        String line   = String.format("%s [%-20s] %-5s %s — %s", ts, shorten(thread, 20), level, name, body);

        PrintStream out = (level == Level.ERROR || level == Level.WARN) ? System.err : System.out;
        out.println(line);
        if (t != null) t.printStackTrace(out);
    }

    private static String format(String msg, Object[] args) {
        if (msg == null) return "null";
        if (args == null || args.length == 0) return msg;
        StringBuilder sb  = new StringBuilder(msg.length() + 32);
        int argIdx = 0;
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '{' && i + 1 < msg.length() && msg.charAt(i + 1) == '}') {
                sb.append(argIdx < args.length ? args[argIdx++] : "{}");
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : "..." + s.substring(s.length() - (max - 3));
    }
}
