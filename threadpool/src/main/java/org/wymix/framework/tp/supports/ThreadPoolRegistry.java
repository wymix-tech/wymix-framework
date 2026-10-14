package org.wymix.framework.tp.supports;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ThreadPoolRegistry {

    public static ExecutorWrapper registerThreadPool(String name, int corePoolSize, int maxPoolSize, long keepAliveSeconds, TimeUnit timeUnit, BlockingQueue<Runnable> queue, RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadFactory threadFactory = new NamedThreadFactory(name);
        ExecutorWrapper threadPoolExecutor = new ExecutorServiceWrapper(corePoolSize, maxPoolSize, keepAliveSeconds, timeUnit, queue, threadFactory, rejectedExecutionHandler);
        ThreadPoolHolder.put(name, threadPoolExecutor);
        return threadPoolExecutor;
    }

    public static ExecutorWrapper newFixedThreadPool(String name, int corePoolSize, int queueCapacity) {
        return registerThreadPool(name, corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.AbortPolicy());
    }

    public static ExecutorWrapper newSingleThreadExecutor(String name, int queueCapacity) {
        return registerThreadPool(name, 1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(queueCapacity), new ThreadPoolExecutor.AbortPolicy());
    }

    public static ExecutorWrapper registerScheduledThreadPool(String name, int corePoolSize, RejectedExecutionHandler rejectedExecutionHandler) {
        ExecutorWrapper threadPoolExecutor = new ScheduledExecutorServiceWrapper(corePoolSize, new NamedThreadFactory(name), rejectedExecutionHandler);
        ThreadPoolHolder.put(name, threadPoolExecutor);
        return threadPoolExecutor;
    }

    public static ExecutorWrapper newScheduledThreadPool(String name, int corePoolSize) {
        return registerScheduledThreadPool(name, corePoolSize, new ThreadPoolExecutor.AbortPolicy());
    }

}
