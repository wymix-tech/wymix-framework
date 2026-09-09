package com.travelsky.base.cache.manager;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CompositeCacheManager 直接单测：双写、读穿透、回源回填、单级失败隔离、
 * evict/clear 双清、getCacheNames 纯净聚合、retrieve 按底层能力降级。
 */
class CompositeCacheManagerTest {

    /** 记录调用次数的内存缓存，便于断言回源/回填行为。 */
    static class CountingCache extends ConcurrentMapCache {
        final AtomicInteger putCount = new AtomicInteger();

        CountingCache(String name) {
            super(name, true);
        }

        @Override
        public void put(Object key, Object value) {
            putCount.incrementAndGet();
            super.put(key, value);
        }
    }

    private static CacheManager managerOf(Cache... caches) {
        SimpleCacheManager m = new SimpleCacheManager();
        List<Cache> list = new ArrayList<>();
        for (Cache c : caches) {
            if (c != null) {
                list.add(c);
            }
        }
        m.setCaches(list);
        m.afterPropertiesSet();
        return m;
    }

    @Test
    void putShouldWriteBothLevels() {
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");

        cache.put("k", "v");

        assertThat(primary.get("k").get()).isEqualTo("v");
        assertThat(local.get("k").get()).isEqualTo("v");
        assertThat(primary.putCount.get()).isEqualTo(1);
        assertThat(local.putCount.get()).isEqualTo(1);
    }

    @Test
    void readThroughPrimaryMissShouldFallbackToLocal() {
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        local.put("k", "local-v");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");

        // 主 miss、次 hit：返回次的值，且不回写主缓存（读路径不产生副作用）
        assertThat(cache.get("k").get()).isEqualTo("local-v");
        assertThat(primary.get("k")).isNull();
    }

    @Test
    void valueLoaderShouldBackfillLocalCache() {
        // 关键防护：回源成功后本地缓存必须被回填（评审变异验证曾暴露该路径零覆盖）
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");

        String value = cache.get("k", () -> "loaded");
        assertThat(value).isEqualTo("loaded");
        assertThat(primary.get("k").get()).isEqualTo("loaded");
        // 本地已被回填
        assertThat(local.get("k")).isNotNull();
        assertThat(local.get("k").get()).isEqualTo("loaded");
    }

    @Test
    void localWriteFailureShouldNotBreakPrimaryWrite() {
        CountingCache primary = new CountingCache("c");
        Cache brokenLocal = new ConcurrentMapCache("c", true) {
            @Override
            public void put(Object key, Object value) {
                throw new IllegalStateException("local down");
            }
        };
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(brokenLocal));
        Cache cache = manager.getCache("c");

        cache.put("k", "v");

        // 主缓存写入不受影响，put 正常返回
        assertThat(primary.get("k").get()).isEqualTo("v");
    }

    @Test
    void evictAndClearShouldApplyToBothLevels() {
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");
        cache.put("k", "v");

        cache.evict("k");
        assertThat(primary.get("k")).isNull();
        assertThat(local.get("k")).isNull();

        cache.put("k2", "v2");
        cache.clear();
        assertThat(primary.get("k2")).isNull();
        assertThat(local.get("k2")).isNull();
    }

    @Test
    void getCacheNamesShouldOnlyAggregateDeclaredNames() {
        CountingCache primary = new CountingCache("declared");
        CountingCache local = new CountingCache("declared");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));

        // 请求一个从未声明过的名字：允许懒建缓存，但绝不能混进 getCacheNames
        manager.getCache("ghost");

        Collection<String> names = manager.getCacheNames();
        assertThat(names).containsExactly("declared");
    }

    @Test
    void constructorShouldRejectNullManagers() {
        CacheManager any = managerOf(new CountingCache("c"));
        assertThatThrownBy(() -> new CompositeCacheManager(null, any))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeCacheManager(any, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retrieveShouldBackfillLocalWhenPrimarySupports() {
        // 主缓存支持 retrieve（ConcurrentMapCache 覆写了该方法）：结果回填本地
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");

        CompletableFuture<Object> future = cache.retrieve("k", () -> CompletableFuture.completedFuture("v"));
        assertThat(future.join()).isEqualTo("v");
        assertThat(primary.get("k").get()).isEqualTo("v");
        assertThat(local.get("k").get()).isEqualTo("v");
    }

    @Test
    void retrieveShouldDegradeWhenPrimaryUnsupported() {
        // 关键防护：主缓存不支持 CompletableFuture 检索（HazelcastCache 场景）时，
        // 必须降级到本地 retrieve 而不是抛 UnsupportedOperationException
        Cache unsupportedPrimary = new ConcurrentMapCache("c", true) {
            @Override
            public <T> CompletableFuture<T> retrieve(Object key,
                    java.util.function.Supplier<CompletableFuture<T>> valueLoader) {
                throw new UnsupportedOperationException("not supported");
            }
        };
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(unsupportedPrimary), managerOf(local));
        Cache cache = manager.getCache("c");

        CompletableFuture<Object> future = cache.retrieve("k", () -> CompletableFuture.completedFuture("v"));
        assertThat(future.join()).isEqualTo("v");
        assertThat(local.get("k").get()).isEqualTo("v");
    }

    @Test
    void retrieveShouldThrowReadableErrorWhenBothUnsupported() {
        Cache unsupportedPrimary = new ConcurrentMapCache("c", true) {
            @Override
            public <T> CompletableFuture<T> retrieve(Object key,
                    java.util.function.Supplier<CompletableFuture<T>> valueLoader) {
                throw new UnsupportedOperationException("primary unsupported");
            }
        };
        Cache unsupportedLocal = new ConcurrentMapCache("c", true) {
            @Override
            public <T> CompletableFuture<T> retrieve(Object key,
                    java.util.function.Supplier<CompletableFuture<T>> valueLoader) {
                throw new UnsupportedOperationException("local unsupported");
            }
        };
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(unsupportedPrimary), managerOf(unsupportedLocal));
        Cache cache = manager.getCache("c");

        assertThatThrownBy(() -> cache.retrieve("k", () -> CompletableFuture.completedFuture("v")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void retrieveSingleArgShouldReturnPresentValue() {
        CountingCache primary = new CountingCache("c");
        CountingCache local = new CountingCache("c");
        CompositeCacheManager manager = new CompositeCacheManager(managerOf(primary), managerOf(local));
        Cache cache = manager.getCache("c");
        cache.put("k", "v");

        // 主命中
        assertThat(cache.retrieve("k").join()).isEqualTo("v");
        // 主 miss、次命中
        primary.evict("k");
        assertThat(cache.retrieve("k").join()).isEqualTo("v");
        // 双 miss
        local.evict("k");
        assertThat(cache.retrieve("k")).isNull();
    }
}
