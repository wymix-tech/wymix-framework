package org.wymix.framework.tp.supports;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;
import java.util.concurrent.*;

/** Non-owning monitor/control adapter. Execution still goes through the Spring owner. */
public final class NativeThreadPoolAdapter extends AbstractExecutorService
        implements ExecutorWrapper {
    private final AsyncTaskExecutor owner;
    private final ThreadPoolExecutor pool;

    public NativeThreadPoolAdapter(AsyncTaskExecutor owner, ThreadPoolExecutor pool) {
        this.owner = owner;
        this.pool = pool;
    }

    public boolean isScheduled() {
        return pool instanceof ScheduledThreadPoolExecutor;
    }

    boolean observes(ThreadPoolExecutor executor) {
        return pool == executor;
    }

    @Override
    public int getPoolSize() {
        return pool.getPoolSize();
    }

    @Override
    public int getCorePoolSize() {
        return pool.getCorePoolSize();
    }

    @Override
    public int getMaxPoolSize() {
        return isScheduled() ? pool.getCorePoolSize() : pool.getMaximumPoolSize();
    }

    @Override
    public void setCorePoolSize(int size) {
        if (owner
                instanceof
                org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor)
            executor.setCorePoolSize(size);
        else if (owner
                instanceof
                org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler)
            scheduler.setPoolSize(size);
        else pool.setCorePoolSize(size);
    }

    @Override
    public void setMaxPoolSize(int size) {
        if (isScheduled()) setCorePoolSize(size);
        else if (owner
                instanceof
                org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor)
            executor.setMaxPoolSize(size);
        else pool.setMaximumPoolSize(size);
    }

    @Override
    public long getKeepAliveTime() {
        return pool.getKeepAliveTime(TimeUnit.NANOSECONDS);
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.NANOSECONDS;
    }

    @Override
    public void setKeepAliveTime(long time, TimeUnit unit) {
        long seconds = unit.toSeconds(time);
        if (owner
                        instanceof
                        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor
                && seconds >= 0
                && seconds <= Integer.MAX_VALUE
                && unit.toNanos(time) == TimeUnit.SECONDS.toNanos(seconds)) {
            executor.setKeepAliveSeconds((int) seconds);
        } else {
            pool.setKeepAliveTime(time, unit);
        }
    }

    @Override
    public long standby() {
        return pool.getQueue().size();
    }

    @Override
    public long running() {
        return pool.getActiveCount();
    }

    @Override
    public long remaining() {
        return standby() + running();
    }

    @Override
    public long total() {
        return pool.getTaskCount();
    }

    @Override
    public boolean finished() {
        return remaining() == 0;
    }

    @Override
    public double processedRate() {
        long n = total();
        return n == 0 ? 1 : Math.min(1D, pool.getCompletedTaskCount() * 1D / n);
    }

    // Native executors cannot expose all business outcomes (e.g. CompletableFuture callbacks).
    // These values are unavailable, not a claim of zero failures or successful execution.
    @Override
    public boolean outcomeMetricsAvailable() {
        return false;
    }

    @Override
    public double successRate() {
        return 0;
    }

    @Override
    public double executionRate() {
        return 0;
    }

    @Override
    public long cost() {
        return 0;
    }

    @Override
    public double avgCost() {
        return 0;
    }

    @Override
    public void execute(Runnable task) {
        owner.execute(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return owner.submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return owner.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return owner.submit(Executors.callable(task, result));
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return pool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return pool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return pool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
