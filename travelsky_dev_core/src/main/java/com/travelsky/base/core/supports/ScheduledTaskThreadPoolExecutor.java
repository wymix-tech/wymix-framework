package com.travelsky.base.core.supports;

import com.travelsky.base.tp.supports.ThreadPoolRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class ScheduledTaskThreadPoolExecutor extends ThreadPoolTaskScheduler {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        return (ScheduledExecutorService) ThreadPoolRegistry.registerScheduledThreadPool(name, poolSize, rejectedExecutionHandler);
    }
}
