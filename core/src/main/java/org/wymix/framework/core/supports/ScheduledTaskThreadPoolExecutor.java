package org.wymix.framework.core.supports;

import org.wymix.framework.tp.supports.ThreadPoolRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class ScheduledTaskThreadPoolExecutor extends ThreadPoolTaskScheduler {
    private String name;
    private org.wymix.framework.tp.supports.ExecutorWrapper managed;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        managed = ThreadPoolRegistry.registerScheduledThreadPool(name, poolSize, rejectedExecutionHandler);
        return (ScheduledExecutorService) managed;
    }    @Override public void destroy() {
        try { super.destroy(); }
        finally { if (managed != null) org.wymix.framework.tp.supports.ThreadPoolHolder.unregister(name, managed); }
    }

}
