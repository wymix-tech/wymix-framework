package com.travelsky.base.core.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

@ConfigurationProperties(prefix = "async")
public class AsyncScheduleProperties implements Serializable {

    private static final int DEFAULT_CORE_POOL_SIZE = 4;
    private static final int DEFAULT_MAX_POOL_SIZE = 8;
    private static final int DEFAULT_QUEUE_CAPACITY = 300;
    private static final boolean DEFAULT_WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN = true;
    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 60;
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;
    private static final String DEFAULT_THREAD_NAME_PREFIX = "application-";

    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;

    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

    private String threadNamePrefix = DEFAULT_THREAD_NAME_PREFIX;

    private boolean waitForTasksToCompleteOnShutdown = DEFAULT_WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN;

    private int awaitTerminationSeconds = DEFAULT_AWAIT_TERMINATION_SECONDS;

    private int keepAliveSeconds = DEFAULT_KEEP_ALIVE_SECONDS;

    private Schedule schedule = new Schedule();

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
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

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public static class Schedule implements Serializable {

        private static final String DEFAULT_SCHEDULE_THREAD_NAME_PREFIX = "application-schedule-";
        private int poolSize = DEFAULT_CORE_POOL_SIZE;

        private String threadNamePrefix = DEFAULT_SCHEDULE_THREAD_NAME_PREFIX;

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }
}
