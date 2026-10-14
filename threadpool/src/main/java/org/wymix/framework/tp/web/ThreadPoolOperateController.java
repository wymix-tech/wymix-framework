package org.wymix.framework.tp.web;


import org.wymix.framework.tp.supports.ThreadPoolContext;
import org.wymix.framework.tp.supports.ThreadPoolMonitor;
import org.wymix.framework.tp.supports.ThreadPoolOperator;

import java.util.concurrent.TimeUnit;

public class ThreadPoolOperateController {

    public boolean changeThreadPoolConfig(String threadName, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit) {
        org.wymix.framework.tp.supports.ExecutorWrapper pool = org.wymix.framework.tp.supports.ThreadPoolHolder.get(threadName);
        if (pool == null) throw new IllegalArgumentException("Unknown thread pool: " + threadName);
        synchronized (pool) {
            int core = corePoolSize == -1 ? pool.getCorePoolSize() : corePoolSize;
            int max = maxPoolSize == -1 ? pool.getMaxPoolSize() : maxPoolSize;
            if (pool instanceof org.wymix.framework.tp.supports.ScheduledExecutorServiceWrapper && core != max) {
                throw new IllegalArgumentException("Scheduled pools require equal core/max sizes");
            }
            if (core < 0 || max <= 0 || core > max || keepAliveTime < -1 || timeUnit == null) {
                throw new IllegalArgumentException("Invalid thread pool configuration");
            }
            if (max > pool.getMaxPoolSize()) pool.setMaxPoolSize(max);
            pool.setCorePoolSize(core);
            if (max != pool.getMaxPoolSize()) pool.setMaxPoolSize(max);
            if (keepAliveTime >= 0) pool.setKeepAliveTime(keepAliveTime, timeUnit);
        }

        return true;
    }

}
