package org.wymix.framework.tp.supports;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.Collection;
import java.util.List;
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

public class ScheduledExecutorServiceWrapper extends AbstractExecutorServiceWrapper
        implements ExecutorWrapper, ScheduledExecutorService {

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private final Object mutex = new Object();

    public ScheduledExecutorServiceWrapper(
            int corePoolSize,
            ThreadFactory threadFactory,
            RejectedExecutionHandler rejectedExecutionHandler) {
        super(
                corePoolSize,
                corePoolSize,
                0,
                TimeUnit.SECONDS,
                null,
                threadFactory,
                rejectedExecutionHandler);
        this.scheduledThreadPoolExecutor =
                new ScheduledThreadPoolExecutor(
                        corePoolSize,
                        threadFactory,
                        (command, pool) -> {
                            rejectedCount.increment();
                            try {
                                rejectedExecutionHandler.rejectedExecution(command, pool);
                            } finally {
                                if (command instanceof Future<?> future) future.cancel(false);
                            }
                        }) {
                    @Override
                    protected <V> java.util.concurrent.RunnableScheduledFuture<V> decorateTask(
                            Runnable runnable,
                            java.util.concurrent.RunnableScheduledFuture<V> task) {
                        Runnable original = com.alibaba.ttl.TtlRunnable.unwrap(runnable);
                        if (original
                                instanceof
                                java.util.concurrent.CompletableFuture.AsynchronousCompletionTask)
                            outcomeMetricsAvailable = false;
                        return new TrackedScheduledFuture<>(
                                task, original instanceof Future<?> future ? future : null);
                    }

                    @Override
                    protected <V> java.util.concurrent.RunnableScheduledFuture<V> decorateTask(
                            Callable<V> callable,
                            java.util.concurrent.RunnableScheduledFuture<V> task) {
                        return new TrackedScheduledFuture<>(task, null);
                    }
                };
        this.scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.executorService =
                TtlExecutors.getTtlScheduledExecutorService(this.scheduledThreadPoolExecutor);
    }

    private final class TrackedScheduledFuture<V>
            implements java.util.concurrent.RunnableScheduledFuture<V> {
        private final java.util.concurrent.RunnableScheduledFuture<V> task;
        private final Future<?> original;
        private final java.util.concurrent.atomic.AtomicBoolean terminal =
                new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.concurrent.atomic.AtomicBoolean cancellationRecorded =
                new java.util.concurrent.atomic.AtomicBoolean();

        TrackedScheduledFuture(
                java.util.concurrent.RunnableScheduledFuture<V> task, Future<?> original) {
            this.task = task;
            this.original = original;
            if (!task.isPeriodic()) count.increment();
        }

        @Override
        public void run() {
            if (task.isCancelled()) return;
            if (task.isPeriodic()) count.increment();
            long start = System.nanoTime();
            task.run();
            if (!task.isPeriodic() && !terminal.compareAndSet(false, true)) return;
            try {
                if (task.isDone()) task.get();
                if (original != null && original.isDone()) original.get();
                successCount.increment();
            } catch (java.util.concurrent.CancellationException ex) {
                recordCancellation();
            } catch (ExecutionException ex) {
                failedCount.increment();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                failedCount.increment();
            } finally {
                cost.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                finished.increment();
            }
        }

        @Override
        public boolean cancel(boolean interrupt) {
            boolean changed = task.cancel(interrupt);
            if (changed) {
                if (original != null) original.cancel(interrupt);
                recordCancellation();
                if (!task.isPeriodic() && terminal.compareAndSet(false, true)) finished.increment();
                scheduledThreadPoolExecutor.remove(this);
            }
            return changed;
        }

        private void recordCancellation() {
            if (cancellationRecorded.compareAndSet(false, true)) cancelledCount.increment();
        }

        @Override
        public boolean isPeriodic() {
            return task.isPeriodic();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return task.compareTo(
                    other
                                    instanceof
                                    ScheduledExecutorServiceWrapper.TrackedScheduledFuture<?>
                                            tracked
                            ? tracked.task
                            : other);
        }
    }

    @Override
    public int getPoolSize() {
        return scheduledThreadPoolExecutor.getPoolSize();
    }

    @Override
    public long remaining() {
        return standby() + running();
    }

    @Override
    public boolean finished() {
        return remaining() == 0;
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
            this.scheduledThreadPoolExecutor.setCorePoolSize(corePoolSize);
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = corePoolSize;
        }
    }

    @Override
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (mutex) {
            setCorePoolSize(maxPoolSize);
        }
    }

    @Override
    public void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        synchronized (mutex) {
            scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveTime, timeUnit);
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }
    }

    @Override
    public void allowCoreThreadTimeOut(boolean allow) {
        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(allow);
    }

    @Override
    public void prestartAllCoreThreads(boolean prestart) {
        if (prestart) scheduledThreadPoolExecutor.prestartAllCoreThreads();
    }

    @Override
    public void shutdown() {
        this.executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> queued = this.executorService.shutdownNow();
        for (Runnable task : queued) if (task instanceof Future<?> future) future.cancel(false);
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
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return this.executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return this.executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return this.executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return this.executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService)
                .schedule(
                        runnableDecorator == null ? command : runnableDecorator.decorate(command),
                        delay,
                        unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService).schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService)
                .scheduleAtFixedRate(
                        runnableDecorator == null ? command : runnableDecorator.decorate(command),
                        initialDelay,
                        period,
                        unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return ((ScheduledExecutorService) this.executorService)
                .scheduleWithFixedDelay(
                        runnableDecorator == null ? command : runnableDecorator.decorate(command),
                        initialDelay,
                        delay,
                        unit);
    }
}
