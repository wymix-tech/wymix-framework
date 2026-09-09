package com.travelsky.base.cache.manager;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 组合 {@link CacheManager}：按装配顺序聚合多个底层 CacheManager。
 *
 * <p>当 classpath 同时存在 Hazelcast 与 Caffeine 时，由
 * {@code CacheManagerConfiguration} 以 {@code CompositeCacheManager(cacheManager Bean)} 的形式
 * 同时启用两级缓存，语义为：
 * <ul>
 *   <li>读：按主次顺序查找，先命中先返回（主 = Hazelcast 分布式，次 = Caffeine 本地）；</li>
 *   <li>写：双写，任一级写入失败不影响另一级（localCache.put 包裹 try-catch）。</li>
 * </ul>
 */
public class CompositeCacheManager implements CacheManager {

    private final CacheManager primaryCacheManager;

    private final CacheManager localCacheManager;

    private final Map<String, CompositeCache> caches = new LinkedHashMap<>();

    public CompositeCacheManager(CacheManager primaryCacheManager, CacheManager localCacheManager) {
        this.primaryCacheManager = primaryCacheManager;
        this.localCacheManager = localCacheManager;
    }

    public CacheManager getPrimaryCacheManager() {
        return primaryCacheManager;
    }

    public CacheManager getLocalCacheManager() {
        return localCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::buildCompositeCache);
    }

    private CompositeCache buildCompositeCache(String name) {
        Cache primary = primaryCacheManager.getCache(name);
        Cache local = localCacheManager.getCache(name);
        if (primary == null && local == null) {
            return null;
        }
        return new CompositeCache(primary, local);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableCollection(caches.keySet());
    }

    /**
     * 组合缓存：读按主->次，写双写（本地写失败不影响分布式写）。
     */
    static class CompositeCache implements Cache {

        private final Cache primaryCache;

        private final Cache localCache;

        CompositeCache(Cache primaryCache, Cache localCache) {
            this.primaryCache = primaryCache;
            this.localCache = localCache;
        }

        Cache getPrimaryCache() {
            return primaryCache;
        }

        Cache getLocalCache() {
            return localCache;
        }

        @Override
        public String getName() {
            return primaryCache != null ? primaryCache.getName()
                    : (localCache != null ? localCache.getName() : null);
        }

        @Override
        public Object getNativeCache() {
            return primaryCache != null ? primaryCache.getNativeCache() : localCache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper wrapper = primaryCache != null ? primaryCache.get(key) : null;
            if (wrapper != null) {
                return wrapper;
            }
            return localCache != null ? localCache.get(key) : null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = primaryCache != null ? primaryCache.get(key, type) : null;
            if (value != null) {
                return value;
            }
            return localCache != null ? localCache.get(key, type) : null;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            T value = primaryCache != null ? primaryCache.get(key, valueLoader) : null;
            if (value != null) {
                return value;
            }
            return localCache != null ? localCache.get(key, valueLoader) : null;
        }

        @Override
        public void put(Object key, Object value) {
            if (primaryCache != null) {
                primaryCache.put(key, value);
            }
            if (localCache != null) {
                try {
                    localCache.put(key, value);
                } catch (RuntimeException ex) {
                    // 本地缓存写入失败不阻断分布式缓存写入
                }
            }
        }

        @Override
        public void evict(Object key) {
            if (primaryCache != null) {
                primaryCache.evict(key);
            }
            if (localCache != null) {
                localCache.evict(key);
            }
        }

        @Override
        public void clear() {
            if (primaryCache != null) {
                primaryCache.clear();
            }
            if (localCache != null) {
                localCache.clear();
            }
        }
    }
}
