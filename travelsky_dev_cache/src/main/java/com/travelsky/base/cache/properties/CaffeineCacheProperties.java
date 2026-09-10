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
