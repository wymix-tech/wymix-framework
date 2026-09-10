package com.travelsky.base.cache.manager;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

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
        org.springframework.util.Assert.notNull(primaryCacheManager, "primaryCacheManager 不能为 null");
        org.springframework.util.Assert.notNull(localCacheManager, "localCacheManager 不能为 null");
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
        // 只聚合两级底层真实声明的命名，不混入 getCache() 的懒加载表，
        // 避免对外暴露业务从未配置过的缓存名（如回退 default 命名空间的名称）
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
                // 主命中时同步回填本地，本地过期/被淘汰后可重新受益于本地读加速
                backfillLocal(key, wrapper.get());
                return wrapper;
            }
            ValueWrapper localWrapper = localCache != null ? localCache.get(key) : null;
            if (localWrapper != null) {
                // 本地命中但主 miss（主缓存 TTL/淘汰后）：回填主缓存，
                // 否则本地残留旧值会被一直读到且永不刷新（写后读不一致）
                backfillPrimary(key, localWrapper.get());
                return localWrapper;
            }
            return null;
        }

        /**
         * 回填主缓存（失败仅 warn 留痕，不影响读路径返回）。
         */
        private void backfillPrimary(Object key, Object value) {
            if (value == null || primaryCache == null) {
                return;
            }
            try {
                primaryCache.put(key, value);
            } catch (RuntimeException ex) {
                log.warn("主缓存回填失败，不影响本次读取，cache={}", getName(), ex);
            }
        }

        /**
         * 回填本地缓存（失败仅 warn 留痕，不影响读路径返回）。
         */
        private void backfillLocal(Object key, Object value) {
            if (value == null || localCache == null) {
                return;
            }
            try {
                localCache.put(key, value);
            } catch (RuntimeException ex) {
                log.warn("本地缓存回填失败，不影响分布式缓存，cache={}", getName(), ex);
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = primaryCache != null ? primaryCache.get(key, type) : null;
            if (value != null) {
                backfillLocal(key, value);
                return value;
            }
            T localValue = localCache != null ? localCache.get(key, type) : null;
            if (localValue != null) {
                backfillPrimary(key, localValue);
                return localValue;
            }
            return null;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            if (primaryCache != null) {
                T value = primaryCache.get(key, valueLoader);
                if (value != null) {
                    // 主命中（含主缓存回源路径）：无论值来自主缓存还是经 valueLoader 回源，
                    // 都同步回填本地缓存。否则 Hazelcast 侧过期后本应能加速的本地缓存
                    // 只在启动后首个 expireInSec 窗口内有效（读路径本地缓存恒失效）
                    backfillLocal(key, value);
                    return value;
                }
                return null;
            }
            return localCache != null ? localCache.get(key, valueLoader) : null;
        }

        /**
         * Spring 6.1 起接口默认实现会抛 {@link UnsupportedOperationException}，
         * 响应式路径必须覆写。
         * <p>按底层能力降级：主缓存不支持 CompletableFuture 检索（如 HazelcastCache
         * 未覆写 {@code retrieve}，调用即抛 {@link UnsupportedOperationException}）时，
         * 降级为 {@code localCache.retrieve}，保证响应式路径可用；
         * 两级都不支持时保留首个可读异常，不静默吞掉。
         */
        @Override
        public <T> CompletableFuture<T> retrieve(
                Object key, Supplier<CompletableFuture<T>> valueSupplier) {
            CompletableFuture<T> future = null;
            UnsupportedOperationException primaryFailure = null;
            if (primaryCache != null) {
                try {
                    future = primaryCache.retrieve(key, valueSupplier);
                } catch (UnsupportedOperationException ex) {
                    primaryFailure = ex;
                }
            }
            if (future == null) {
                if (localCache != null) {
                    // 本地缓存（如 CaffeineCache）支持 retrieve 时委托，
                    // 结果非空时回填主缓存（与 get(Object) 的主 miss 本地命中语义一致）
                    try {
                        return localCache.retrieve(key, valueSupplier)
                                .thenApply(value -> {
                                    if (value != null) {
                                        backfillPrimary(key, value);
                                    }
                                    return value;
                                });
                    } catch (UnsupportedOperationException ex) {
                        // 本地也不支持：先回填本地（同 get(key, valueLoader) 语义），再抛可读异常
                        if (primaryFailure != null) {
                            fillLocal(key, valueSupplier);
                        }
                        throw ex;
                    }
                }
                if (primaryFailure != null) {
                    throw primaryFailure;
                }
                return null;
            }
            return future.thenApply(value -> {
                if (value != null) {
                    backfillLocal(key, value);
                }
                return value;
            });
        }

        /**
         * 两级均不支持 CompletableFuture 检索时的兜底：以阻塞方式回源并回填本地缓存，
         * 保证缓存链路仍能工作，随后将原始异常抛给调用方。
         */
        private <T> void fillLocal(Object key, Supplier<CompletableFuture<T>> valueSupplier) {
            try {
                T value = valueSupplier.get().get();
                if (value != null) {
                    put(key, value);
                }
            } catch (InterruptedException ex) {
                // 仅响应中断时恢复中断标志，避免把正常回源失败也标记为中断（中断标志粘性
                // 会污染后续所有阻塞操作）；ExecutionException 归入通用失败仅留痕
                Thread.currentThread().interrupt();
                log.warn("retrieve 降级回源被中断，cache={}", getName(), ex);
            } catch (java.util.concurrent.ExecutionException ex) {
                log.warn("retrieve 降级回源失败，cache={}", getName(), ex);
            } catch (RuntimeException ex) {
                log.warn("retrieve 降级回源失败，cache={}", getName(), ex);
            }
        }

        /**
         * 单参版本同理按底层能力降级：主命中解包（可能为 ValueWrapper）后返回并回填本地；
         * 主 miss 或主不支持时降级查本地；两级都不支持时保留首个可读异常。
         */
        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<Object> retrieve(Object key) {
            CompletableFuture<Object> future = null;
            UnsupportedOperationException primaryFailure = null;
            if (primaryCache != null) {
                try {
                    future = (CompletableFuture<Object>) primaryCache.retrieve(key);
                } catch (UnsupportedOperationException ex) {
                    primaryFailure = ex;
                }
            }
            if (future == null) {
                // 底层单参 retrieve 对 miss 的表达可能是返回 null future（如 ConcurrentMapCache），
                // 也可能抛 UnsupportedOperationException（如 HazelcastCache），两种都处理
                if (localCache != null) {
                    CompletableFuture<Object> localFuture =
                            (CompletableFuture<Object>) localCache.retrieve(key);
                    if (localFuture == null) {
                        return null;
                    }
                    return localFuture.thenApply(value -> {
                        Object actual = unwrap(value);
                        if (actual != null) {
                            backfillPrimary(key, actual);
                        }
                        return actual;
                    });
                }
                if (primaryFailure != null) {
                    throw primaryFailure;
                }
                return null;
            }
            return future.thenCompose(value -> {
                Object actual = unwrap(value);
                if (actual != null) {
                    backfillLocal(key, actual);
                    return CompletableFuture.completedFuture(actual);
                }
                // 主 miss：降级查本地，本地命中回填主缓存（写后读一致性）
                if (localCache != null) {
                    CompletableFuture<Object> localFuture =
                            (CompletableFuture<Object>) localCache.retrieve(key);
                    if (localFuture == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return localFuture.thenApply(localValue -> {
                        Object localActual = unwrap(localValue);
                        if (localActual != null) {
                            backfillPrimary(key, localActual);
                        }
                        return localActual;
                    });
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        /**
         * 单参 retrieve 的部分底层实现（如 ConcurrentMapCache）返回的是
         * {@link Cache.ValueWrapper}，需解包后再判断命中与回填。
         */
        private Object unwrap(Object value) {
            return (value instanceof Cache.ValueWrapper)
                    ? ((Cache.ValueWrapper) value).get() : value;
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
                try {
                    localCache.evict(key);
                } catch (RuntimeException ex) {
                    // 与 put 策略一致：本地失败不阻断业务，但留痕。
                    // 注意两级短暂不一致（本地可能残留已被驱逐的旧值），
                    // 会被读路径的互相回填机制收敛（主 miss 时以主为准重建本地）
                    log.warn("本地缓存驱逐失败，cache={}", getName(), ex);
                }
            }
        }

        @Override
        public void clear() {
            if (primaryCache != null) {
                primaryCache.clear();
            }
            if (localCache != null) {
                try {
                    localCache.clear();
                } catch (RuntimeException ex) {
                    log.warn("本地缓存清空失败，cache={}", getName(), ex);
                }
            }
        }
    }
}
