package jxmvc.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class JxCache {
    private static volatile JxCacheBackend globalBackend = null;

    public static void setGlobalBackend(JxCacheBackend backend) {
        globalBackend = backend;
    }

    public static JxCacheBackend getGlobalBackend() {
        return globalBackend;
    }

    private static final ConcurrentHashMap<String, JxCache> REGISTRY = new ConcurrentHashMap<>();

    private static final java.util.concurrent.ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jx-cache-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        CLEANER.scheduleWithFixedDelay(JxCache::cleanAll, 60, 60, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        CLEANER.shutdownNow();
    }

    public static JxCache get(String name) {
        return REGISTRY.computeIfAbsent(name, k -> new JxCache(k));
    }

    public static void evictAll(String name) {
        if (globalBackend != null) { globalBackend.clear(name); return; }
        JxCache c = REGISTRY.get(name);
        if (c != null) c.clear();
    }

    public static void cleanAll() {
        if (globalBackend != null) return;
        REGISTRY.values().forEach(JxCache::cleanExpired);
    }

    private static final int MAX_ENTRIES =
            BaseDbResolver.propertyInt("jxmvc.cache.maxEntries", 10_000);

    private final String name;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private JxCache(String name) { this.name = name; }

    public JxCache put(String key, Object value) {
        return put(key, value, 0);
    }

    public JxCache put(String key, Object value, long ttlSeconds) {
        if (globalBackend != null) { globalBackend.put(name, key, value, ttlSeconds); return this; }
        long expireAt = ttlSeconds > 0
                ? System.currentTimeMillis() + ttlSeconds * 1000
                : 0;
        ensureCapacity(key);
        store.put(key, new Entry(value, expireAt));
        return this;
    }

    private void ensureCapacity(String key) {
        if (MAX_ENTRIES <= 0 || store.size() < MAX_ENTRIES || store.containsKey(key)) return;
        cleanExpired();
        while (store.size() >= MAX_ENTRIES) {
            String coldest = coldestKey();
            if (coldest == null) return;
            store.remove(coldest);
            locks.remove(coldest);
        }
    }

    private static final int EVICT_SAMPLE = 64;

    private String coldestKey() {
        String coldest = null;
        long oldest = Long.MAX_VALUE;
        int seen = 0;
        for (var e : store.entrySet()) {
            long access = e.getValue().lastAccess;
            if (access < oldest) { oldest = access; coldest = e.getKey(); }
            if (++seen >= EVICT_SAMPLE) break;
        }
        return coldest;
    }

    @SuppressWarnings("unchecked")
    public <T> T fetch(String key, Class<T> type) {
        if (globalBackend != null) {
            Object v = globalBackend.fetch(name, key);
            return type.isInstance(v) ? (T) v : null;
        }
        Entry e = liveEntry(key);
        if (e == null) return null;
        Object v = e.value();
        return type.isInstance(v) ? (T) v : null;
    }

    public Object fetch(String key) {
        if (globalBackend != null) return globalBackend.fetch(name, key);
        Entry e = liveEntry(key);
        return e == null ? null : e.value();
    }

    public boolean has(String key) {
        if (globalBackend != null) return globalBackend.has(name, key);
        return liveEntry(key) != null;
    }

    private Entry liveEntry(String key) {
        Entry e = store.get(key);
        if (e == null) return null;
        if (e.isExpired()) { store.remove(key); return null; }
        e.lastAccess = System.currentTimeMillis();
        return e;
    }

    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(String key, long ttlSeconds, java.util.concurrent.Callable<T> loader) {
        Object cached = fetch(key);
        if (cached != null) return (T) cached;
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = fetch(key);
            if (cached != null) return (T) cached;
            T value = loader.call();
            if (value != null) put(key, value, ttlSeconds);
            return value;
        } catch (Exception e) {
            throw e instanceof RuntimeException r ? r : new JxException(500, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public JxCache evict(String key) {
        if (globalBackend != null) { globalBackend.evict(name, key); return this; }
        store.remove(key);
        locks.remove(key);
        return this;
    }

    public JxCache clear() {
        if (globalBackend != null) { globalBackend.clear(name); return this; }
        store.clear();
        locks.clear();
        return this;
    }

    public int size() { return store.size(); }

    public String name() { return name; }

    private void cleanExpired() {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
        locks.entrySet().removeIf(e -> !store.containsKey(e.getKey())
                && !e.getValue().isLocked()
                && !e.getValue().hasQueuedThreads());
    }

    private static final class Entry {
        final Object value;
        final long expireAt;
        volatile long lastAccess = System.currentTimeMillis();

        Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        Object value() { return value; }

        boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }
}
