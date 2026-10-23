package jxmvc.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class JxRateLimiter {
    private JxRateLimiter() {}

    private static final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    private static final java.util.concurrent.ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jx-ratelimiter-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        CLEANER.scheduleWithFixedDelay(JxRateLimiter::cleanupExpired, 10, 10, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        CLEANER.shutdownNow();
    }

    public static boolean allow(String clientKey, String routeKey, int maxReqs, long windowSecs) {
        String key   = clientKey + "|" + routeKey;
        long   now   = System.currentTimeMillis();
        long   winMs = Math.max(1, windowSecs * 1000);
        long   winStart = (now / winMs) * winMs;

        long[] bucket = buckets.computeIfAbsent(key, k -> new long[]{0, winStart, 0, winMs});
        synchronized (bucket) {
            roll(bucket, winStart, winMs);
            if (estimatedUsage(bucket, now, winMs) >= maxReqs) return false;
            bucket[0]++;
            return true;
        }
    }

    public static long remaining(String clientKey, String routeKey, int maxReqs, long windowSecs) {
        String key   = clientKey + "|" + routeKey;
        long   now   = System.currentTimeMillis();
        long   winMs = Math.max(1, windowSecs * 1000);
        long   winStart = (now / winMs) * winMs;
        long[] bucket = buckets.get(key);
        if (bucket == null) return maxReqs;
        synchronized (bucket) {
            roll(bucket, winStart, winMs);
            return Math.max(0, maxReqs - (long) Math.floor(estimatedUsage(bucket, now, winMs)));
        }
    }

    static void roll(long[] bucket, long winStart, long winMs) {
        long prevStart = bucket[1];
        if (winStart == prevStart) return;
        if (winStart - prevStart == winMs) {
            bucket[2] = bucket[0];
        } else {
            bucket[2] = 0;
        }
        bucket[0] = 0;
        bucket[1] = winStart;
        bucket[3] = winMs;
    }

    static double estimatedUsage(long[] bucket, long now, long winMs) {
        double elapsed = now - bucket[1];
        double weight  = Math.max(0.0, (winMs - elapsed) / (double) winMs);
        return bucket[2] * weight + bucket[0];
    }

    public static void cleanup(long windowSecs) {
        long cutoff = System.currentTimeMillis() - windowSecs * 1000;
        buckets.entrySet().removeIf(e -> {
            long[] b = e.getValue();
            synchronized (b) { return b[1] < cutoff; }
        });
    }

    static void cleanupExpired() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> {
            long[] b = e.getValue();
            synchronized (b) { return now - b[1] >= 2 * b[3]; }
        });
    }
}
