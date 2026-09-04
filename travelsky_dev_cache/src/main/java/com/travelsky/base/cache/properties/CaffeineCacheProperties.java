package com.travelsky.base.cache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "caffeine")
public class CaffeineCacheProperties {

    private Map<String, CaffeineConfig> cache;

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
        private long maximumSize = 999;
        private long maximumWeight = -1;
        private int initialCapacity = 16;
        private boolean allowNullValues = true;
        private long expireInSec = 60 * 10;
        private Policy policy = Policy.AFTER_WRITE;
        private long refreshAfterWriterInSec = -1;

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
    }
}
