package org.wymix.framework.tp.supports;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExecutorServiceWrapper extends AbstractExecutorServiceWrapper implements ExecutorWrapper {

    private final ThreadPoolExecutor threadPoolExecutor;

    private final Object mutex = new Object();

    public ExecutorServiceWrapper(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queue, threadFactory, rejectedExecutionHandler);
        this.threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queue, threadFactory,
                (command, pool) -> {
                    rejectedCount.increment();
                    RunnableWrapper tracked = (RunnableWrapper) command;
                    try { rejectedExecutionHandler.rejectedExecution(command, pool); }
                    finally {
                        if (!tracked.started()) { count.decrement(); tracked.discard(); }
                    }
                }) {
            @Override public void execute(Runnable command) {
                Runnable original = com.alibaba.ttl.TtlRunnable.unwrap(command);
                Future<?> future = original instanceof Future<?> f ? f : null;
                if (runnableDecorator != null) command = runnableDecorator.decorate(command);
                count.increment();
                super.execute(new RunnableWrapper(command, future, finished, cost, successCount, failedCount, cancelledCount));
            }
        };

        this.executorService = TtlExecutors.getTtlExecutorService(threadPoolExecutor);

    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        synchronized (mutex) {
            this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            this.corePoolSize = corePoolSize;
        }
    }

    @Override
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (mutex) {
            this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
            this.maxPoolSize = maxPoolSize;
        }
    }

    @Override
    public void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        synchronized (mutex) {
            this.threadPoolExecutor.setKeepAliveTime(keepAliveTime, timeUnit);
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }
    }

    @Override
    public void shutdown() {
        this.executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> queued = this.executorService.shutdownNow();
        for (Runnable task : queued) {
            if (task instanceof RunnableWrapper tracked && !tracked.started()) {
                tracked.discard(); cancelledCount.increment(); finished.increment();
            }
        }
        return queued;
    }

    @Override
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
    }

    @Override
    public void prestartAllCoreThreads(boolean prestartAllCoreThreads) {
        if (prestartAllCoreThreads) {
            this.threadPoolExecutor.prestartAllCoreThreads();
        }
    }
}
