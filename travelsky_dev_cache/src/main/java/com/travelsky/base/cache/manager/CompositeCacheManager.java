package com.travelsky.base.cache.manager;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private static final Logger log = LoggerFactory.getLogger(CompositeCacheManager.class);

    private final CacheManager primaryCacheManager;

    private final CacheManager localCacheManager;

    private final ConcurrentMap<String, CompositeCache> caches = new ConcurrentHashMap<>();

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
        CompositeCache cache = caches.get(name);
        if (cache == null) {
            // buildCompositeCache 可能返回 null，ConcurrentHashMap.computeIfAbsent 不接受 null 映射值，
            // 因此拆成 get + putIfAbsent；并发下由 putIfAbsent 保证只有一个实例生效
            CompositeCache created = buildCompositeCache(name);
            if (created == null) {
                return null;
            }
            CompositeCache prev = caches.putIfAbsent(name, created);
            cache = (prev != null) ? prev : created;
        }
        return cache;
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
        // 聚合两级底层 CacheManager 的命名（注意两级命名空间不同：Hazelcast 侧为 <app>:<name>）
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (primaryCacheManager.getCacheNames() != null) {
            names.addAll(primaryCacheManager.getCacheNames());
        }
        if (localCacheManager.getCacheNames() != null) {
            names.addAll(localCacheManager.getCacheNames());
        }
        names.addAll(caches.keySet());
        return Collections.unmodifiableSet(names);
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
            if (primaryCache != null) {
                T value = primaryCache.get(key, valueLoader);
                if (value != null) {
                    // 回源成功后同步回填本地缓存，保证「本地读加速」对回源路径同样成立
                    if (localCache != null) {
                        try {
                            localCache.put(key, value);
                        } catch (RuntimeException ex) {
                            log.warn("本地缓存回填失败，不影响分布式缓存，cache={}", getName(), ex);
                        }
                    }
                    return value;
                }
                return null;
            }
            return localCache != null ? localCache.get(key, valueLoader) : null;
        }

        /**
         * Spring 6.1 起接口默认实现会抛 {@link UnsupportedOperationException}，
         * 响应式路径（ReactiveCacheManager 场景）必须覆写：主命中时同步回填本地缓存。
         */
        @Override
        public <T> java.util.concurrent.CompletableFuture<T> retrieve(
                Object key, java.util.function.Supplier<java.util.concurrent.CompletableFuture<T>> valueSupplier) {
            java.util.concurrent.CompletableFuture<T> future =
                    primaryCache != null ? primaryCache.retrieve(key, valueSupplier) : null;
            if (future != null) {
                return future.thenApply(value -> {
                    if (value != null && localCache != null) {
                        try {
                            localCache.put(key, value);
                        } catch (RuntimeException ex) {
                            log.warn("本地缓存回填失败，不影响分布式缓存，cache={}", getName(), ex);
                        }
                    }
                    return value;
                });
            }
            return localCache != null ? localCache.retrieve(key, valueSupplier) : null;
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
                    // 本地缓存写入失败不阻断分布式缓存写入，但留痕便于排障
                    log.warn("本地缓存写入失败，不影响分布式缓存，cache={}", getName(), ex);
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
