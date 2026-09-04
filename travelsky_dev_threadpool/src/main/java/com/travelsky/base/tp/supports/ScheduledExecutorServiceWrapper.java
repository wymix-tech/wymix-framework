package com.travelsky.base.tp.supports;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledExecutorServiceWrapper extends AbstractExecutorServiceWrapper implements ExecutorWrapper, ScheduledExecutorService {

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private final Object mutex = new Object();

    public ScheduledExecutorServiceWrapper(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(corePoolSize, corePoolSize, 0, TimeUnit.SECONDS, null, threadFactory, rejectedExecutionHandler);
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, new RejectedExecutionHandlerWrapper(rejectedExecutionHandler, this.rejectedCount)) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.schedule(new RunnableWrapper(command, finished, cost, successCount), delay, unit);
            }

            @Override
            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                count.increment();
                return super.schedule(() -> {
                    long cur = System.currentTimeMillis();
                    try {
                        V v = callable.call();
                        successCount.increment();
                        return v;
                    } finally {
                        cost.add(System.currentTimeMillis() - cur);
                        finished.increment();
                    }

                }, delay, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.scheduleAtFixedRate(new RunnableWrapper(command, finished, cost, successCount), initialDelay, period, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.scheduleWithFixedDelay(new RunnableWrapper(command, finished, cost, successCount), initialDelay, delay, unit);
            }
        };
        this.executorService = TtlExecutors.getTtlScheduledExecutorService(this.scheduledThreadPoolExecutor);
    }

    private ScheduledExecutorServiceWrapper(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queue, threadFactory, rejectedExecutionHandler);
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, new RejectedExecutionHandlerWrapper(rejectedExecutionHandler, this.rejectedCount)) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.schedule(new RunnableWrapper(command, finished, cost, successCount), delay, unit);
            }

            @Override
            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                count.increment();
                return super.schedule(() -> {
                    long cur = System.currentTimeMillis();
                    try {
                        V v = callable.call();
                        successCount.increment();
                        return v;
                    } finally {
                        cost.add(System.currentTimeMillis() - cur);
                        finished.increment();
                    }

                }, delay, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.scheduleAtFixedRate(new RunnableWrapper(command, finished, cost, successCount), initialDelay, period, unit);
            }

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                if (runnableDecorator != null) {
                    command = runnableDecorator.decorate(command);
                }
                count.increment();
                return super.scheduleWithFixedDelay(new RunnableWrapper(command, finished, cost, successCount), initialDelay, delay, unit);
            }
        };
        this.executorService = TtlExecutors.getTtlScheduledExecutorService(this.scheduledThreadPoolExecutor);
    }

    @Override
    public long standby() {
        return this.scheduledThreadPoolExecutor.getQueue().size();
    }

    @Override
    public long running() {
        return this.scheduledThreadPoolExecutor.getActiveCount();
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        synchronized (mutex) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = corePoolSize;
            this.scheduledThreadPoolExecutor.setCorePoolSize(corePoolSize);
        }
    }

    @Override
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (mutex) {
            this.corePoolSize = maxPoolSize;
            this.maxPoolSize = maxPoolSize;
            this.scheduledThreadPoolExecutor.setMaximumPoolSize(maxPoolSize);
        }
    }

    @Override
    public void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {

    }

    @Override
    public void shutdown() {
        this.executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.executorService.shutdownNow();
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
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService).schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService).schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
