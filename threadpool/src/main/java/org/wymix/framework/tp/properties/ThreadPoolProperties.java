package org.wymix.framework.tp.properties;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolProperties {

    private Map<String, CoreParams> pool;
    private Map<String, CoreParams> scheduled;

    public Map<String, CoreParams> getPool() {
        return pool;
    }

    public void setPool(Map<String, CoreParams> pool) {
        this.pool = pool;
    }

    public Map<String, CoreParams> getScheduled() {
        return scheduled;
    }

    public void setScheduled(Map<String, CoreParams> scheduled) {
        this.scheduled = scheduled;
    }

    public enum RejectedPolicy {
        ABORT_POLICY(ThreadPoolExecutor.AbortPolicy.class.getName()),
        DISCARD_POLICY(ThreadPoolExecutor.DiscardPolicy.class.getName()),
        DISCARD_OLDEST_POLICY(ThreadPoolExecutor.DiscardOldestPolicy.class.getName()),
        CALLER_RUNS_POLICY(ThreadPoolExecutor.CallerRunsPolicy.class.getName()),
        CUSTOM_POLICY(""),
        ;

        private final String clazzName;

        RejectedPolicy(String clazzName) {
            this.clazzName = clazzName;
        }

        public String getClazzName() {
            return clazzName;
        }
    }

    public static class CoreParams {
        private int corePoolSize = 4;
        private int maxPoolSize = 2 * corePoolSize;
        private int queueCapacity = 100;
        private long keepAliveSeconds = 60;
        private RejectedPolicy rejectedPolicy = RejectedPolicy.ABORT_POLICY;
        private String rejectedPolicyClassName;

        public String getRejectedPolicyClassName() {
            return rejectedPolicy == RejectedPolicy.CUSTOM_POLICY ? rejectedPolicyClassName : rejectedPolicy.getClazzName();
        }

        public void setRejectedPolicyClassName(String rejectedPolicyClassName) {
            this.rejectedPolicyClassName = rejectedPolicyClassName;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return Math.max(this.corePoolSize, this.maxPoolSize);
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public RejectedPolicy getRejectedPolicy() {
            return rejectedPolicy;
        }

        public void setRejectedPolicy(RejectedPolicy rejectedPolicy) {
            this.rejectedPolicy = rejectedPolicy;
        }
    }
}
