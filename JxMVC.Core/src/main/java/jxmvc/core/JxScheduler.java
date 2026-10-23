package jxmvc.core;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static void start() {
        for (Object service : JxServiceRegistry.values()) {
            scanAndSchedule(service);
        }
    }

    public static void schedule(Object target, Method method, JxMapping.JxScheduled ann) {
        method.setAccessible(true);
        Runnable task = () -> {
            try { method.invoke(target); }
            catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warn("@JxScheduled {}.{} falló: {}",
                        target.getClass().getSimpleName(), method.getName(), cause.getMessage());
            }
        };
        String cron = ann.cron().trim();
        if (!cron.isEmpty()) {
            scheduleCron(task, cron);
            return;
        }
        long initial = ann.initialDelay();
        if (ann.fixedDelay() > 0) {
            exec.scheduleWithFixedDelay(task, initial, ann.fixedDelay(), TimeUnit.MILLISECONDS);
        } else if (ann.fixedRate() > 0) {
            exec.scheduleAtFixedRate(task, initial, ann.fixedRate(), TimeUnit.MILLISECONDS);
        }
    }

    public static void scheduleCron(Runnable task, String cron) {
        CronTrigger trigger = new CronTrigger(cron);
        fireCron(task, cron, trigger);
    }

    public static void scheduleAtFixedRate(Runnable task, long initialMs, long periodMs) {
        exec.scheduleAtFixedRate(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("scheduleAtFixedRate tarea falló: {}", e.getMessage()); }
        }, initialMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public static void scheduleWithFixedDelay(Runnable task, long initialMs, long delayMs) {
        exec.scheduleWithFixedDelay(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("scheduleWithFixedDelay tarea falló: {}", e.getMessage()); }
        }, initialMs, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void runOnce(Runnable task, long delayMs) {
        exec.schedule(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("runOnce tarea falló: {}", e.getMessage()); }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void runAsync(Runnable task) {
        exec.submit(() -> {
            try { task.run(); }
            catch (Exception e) { log.warn("runAsync tarea falló: {}", e.getMessage()); }
        });
    }

    public static void shutdown() {
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        finally { if (!exec.isTerminated()) exec.shutdownNow(); }
    }

    public static boolean isRunning() {
        return !exec.isShutdown();
    }

    private static void fireCron(Runnable task, String cron, CronTrigger trigger) {
        long delay = trigger.nextDelayMs();
        exec.schedule(() -> {
            try   { task.run(); }
            catch (Exception e) { log.warn("cron \"{}\" falló: {}", cron, e.getMessage()); }
            finally { fireCron(task, cron, trigger); }
        }, delay, TimeUnit.MILLISECONDS);
    }

    static final class CronTrigger {
        private final int[] minutes;
        private final int[] hours;
        private final int[] doms;
        private final int[] months;
        private final int[] dows;

        CronTrigger(String expr) {
            String[] p = expr.trim().split("\\s+");
            if (p.length != 5)
                throw new IllegalArgumentException("Cron debe tener 5 campos: \"" + expr + "\"");
            minutes = parseField(p[0],  0, 59);
            hours   = parseField(p[1],  0, 23);
            doms    = parseField(p[2],  1, 31);
            months  = parseField(p[3],  1, 12);
            dows    = parseField(p[4],  0,  6);
        }

        long nextDelayMs() {
            LocalDateTime now  = LocalDateTime.now();
            LocalDateTime next = nextFire(now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1));
            return Duration.between(now, next).toMillis();
        }

        private LocalDateTime nextFire(LocalDateTime from) {
            LocalDateTime t     = from;
            LocalDateTime limit = from.plusYears(4);
            while (t.isBefore(limit)) {
                if (!has(months, t.getMonthValue())) {
                    t = t.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
                    continue;
                }
                int dow = t.getDayOfWeek().getValue() % 7;
                if (!has(doms, t.getDayOfMonth()) || !has(dows, dow)) {
                    t = t.plusDays(1).withHour(0).withMinute(0);
                    continue;
                }
                if (!has(hours, t.getHour())) {
                    t = t.plusHours(1).withMinute(0);
                    continue;
                }
                if (!has(minutes, t.getMinute())) {
                    t = t.plusMinutes(1);
                    continue;
                }
                return t;
            }
            throw new IllegalStateException("Cron sin próximo disparo en 4 años desde: " + from);
        }

        private static boolean has(int[] arr, int val) {
            for (int a : arr) if (a == val) return true;
            return false;
        }

        private static int[] parseField(String f, int lo, int hi) {
            if ("*".equals(f)) return range(lo, hi);
            if (f.startsWith("*/")) {
                int step = Integer.parseInt(f.substring(2));
                List<Integer> vals = new ArrayList<>();
                for (int i = lo; i <= hi; i += step) vals.add(i);
                return vals.stream().mapToInt(x -> x).toArray();
            }
            if (f.contains(",")) {
                return Arrays.stream(f.split(",")).mapToInt(Integer::parseInt).toArray();
            }
            if (f.contains("-")) {
                String[] parts = f.split("-", 2);
                return range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            return new int[]{ Integer.parseInt(f) };
        }

        private static int[] range(int from, int to) {
            int[] arr = new int[to - from + 1];
            for (int i = 0; i < arr.length; i++) arr[i] = from + i;
            return arr;
        }
    }

    private static void scanAndSchedule(Object target) {
        for (Method m : target.getClass().getMethods()) {
            JxMapping.JxScheduled ann = m.getAnnotation(JxMapping.JxScheduled.class);
            if (ann == null) continue;
            boolean hasCron  = !ann.cron().isBlank();
            boolean hasTimer = ann.fixedDelay() > 0 || ann.fixedRate() > 0;
            if (!hasCron && !hasTimer) continue;
            schedule(target, m, ann);
        }
    }
}
