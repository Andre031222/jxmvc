package jxmvc.core;

public interface JxCacheBackend {
    void put(String cache, String key, Object value, long ttlSeconds);

    Object fetch(String cache, String key);

    void evict(String cache, String key);

    void clear(String cache);

    boolean has(String cache, String key);
}
