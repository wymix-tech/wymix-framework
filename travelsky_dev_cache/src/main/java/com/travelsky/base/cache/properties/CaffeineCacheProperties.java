package com.travelsky.base.cache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "caffeine")
public class CaffeineCacheProperties {

    /**
     * 是否启用 Caffeine 异步缓存模式（{@code AsyncCache}），默认开启。
     *
     * <p>Spring 6.1 起 {@code Cache#retrieve(Object)} / {@code Cache#retrieve(Object, Supplier)}
     * 只对异步缓存形态可用；开启后普通读写行为不变，仅额外获得 {@code retrieve} 能力。
     * 代价是该模式下不能设置 {@code CacheLoader}（Caffeine 的 {@code AsyncCacheLoader}
     * 与 {@code CacheLoader} 互斥），因此本模块不支持 {@code refreshAfterWriterInSec}
     * （需要 LoadingCache，见该项说明）。
     */
    private boolean asyncCacheMode = true;

    private Map<String, CaffeineConfig> cache;

    /**
     * 未在 {@code caffeine.cache.<name>} 中声明规格的缓存名，其默认最大条目数。
     *
     * <p>默认 {@code 10000}（保守上限），避免业务只写 {@code @Cacheable(cacheNames="xxx")}
     * 却未声明规格时开出无界本地缓存 —— 在 TTL 窗口内若 key 基数大（例如把请求参数拼进 key），
     * 进程内存会无上限增长，是 OOM 风险点。
     *
     * <p>设为 {@code -1} 可显式恢复「默认规格不限容量」，此时请确保调用方会在
     * {@code caffeine.cache} 中为每个缓存名声明规格。
     */
    private long defaultMaximumSize = 10000;

    public long getDefaultMaximumSize() {
        return defaultMaximumSize;
    }

    public void setDefaultMaximumSize(long defaultMaximumSize) {
        this.defaultMaximumSize = defaultMaximumSize;
    }

    public boolean isAsyncCacheMode() {
        return asyncCacheMode;
    }

    public void setAsyncCacheMode(boolean asyncCacheMode) {
        this.asyncCacheMode = asyncCacheMode;
    }

    public Map<String, CaffeineConfig> getCache() {
        return cache;
    }

    public void setCache(Map<String, CaffeineConfig> cache) {
        this.cache = cache;
    }

    public enum Policy {
        AFTER_WRITE, AFTER_ACCESS,
    }

    public static class CaffeineConfig {
        /** 未声明时保持 -1，由上层按 {@code caffeine.defaultMaximumSize} 兜底（默认为 10000）。 */
        private long maximumSize = -1;
        private long maximumWeight = -1;
        private int initialCapacity = 16;
        private boolean allowNullValues = true;
        private long expireInSec = 60 * 10;
        private Policy policy = Policy.AFTER_WRITE;
        private long refreshAfterWriterInSec = -1;
        /**
         * 权重函数（Caffeine {@code Weigher<K, V>} 实现类的全限定类名），maximumWeight > 0 时必填。
         */
        private String weigher;

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public long getMaximumWeight() {
            return maximumWeight;
        }

        public void setMaximumWeight(long maximumWeight) {
            this.maximumWeight = maximumWeight;
        }

        public int getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public boolean isAllowNullValues() {
            return allowNullValues;
        }

        public void setAllowNullValues(boolean allowNullValues) {
            this.allowNullValues = allowNullValues;
        }

        public long getExpireInSec() {
            return expireInSec;
        }

        public void setExpireInSec(long expireInSec) {
            this.expireInSec = expireInSec;
        }

        public Policy getPolicy() {
            return policy;
        }

        public void setPolicy(Policy policy) {
            this.policy = policy;
        }

        public long getRefreshAfterWriterInSec() {
            return refreshAfterWriterInSec;
        }

        public void setRefreshAfterWriterInSec(long refreshAfterWriterInSec) {
            this.refreshAfterWriterInSec = refreshAfterWriterInSec;
        }

        public String getWeigher() {
            return weigher;
        }

        public void setWeigher(String weigher) {
            this.weigher = weigher;
        }
    }
}
