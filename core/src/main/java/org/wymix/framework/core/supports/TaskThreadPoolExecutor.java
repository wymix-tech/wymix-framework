package org.wymix.framework.core.supports;

import org.wymix.framework.tp.supports.ExecutorServiceWrapper;
import org.wymix.framework.tp.supports.ThreadPoolRegistry;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskThreadPoolExecutor extends ExecutorConfigurationSupport
        implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

    private final Object poolSizeMonitor = new Object();
    // Runnable decorator to user-level FutureTask, if different
    private final Map<Runnable, Object> decoratedTaskMap =
            new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);
    private int corePoolSize = 1;
    private int maxPoolSize = Integer.MAX_VALUE;
    private int keepAliveSeconds = 60;
    private int queueCapacity = Integer.MAX_VALUE;
    private boolean allowCoreThreadTimeOut = false;
    private boolean prestartAllCoreThreads = false;
    @Nullable
    private TaskDecorator taskDecorator;
    @Nullable
    private ExecutorServiceWrapper threadPoolExecutor;

    /**
     * Return the ThreadPoolExecutor's core pool size.
     */
    public int getCorePoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.corePoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's core pool size.
     * Default is 1.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setCorePoolSize(int corePoolSize) {
        synchronized (this.poolSizeMonitor) {
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            }
            this.corePoolSize = corePoolSize;
        }
    }

    /**
     * Return the ThreadPoolExecutor's maximum pool size.
     */
    public int getMaxPoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.maxPoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's maximum pool size.
     * Default is {@code Integer.MAX_VALUE}.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (this.poolSizeMonitor) {
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setMaxPoolSize(maxPoolSize);
            }
            this.maxPoolSize = maxPoolSize;
        }
    }

    /**
     * Return the ThreadPoolExecutor's keep-alive seconds.
     */
    public int getKeepAliveSeconds() {
        synchronized (this.poolSizeMonitor) {
            return this.keepAliveSeconds;
        }
    }

    /**
     * Set the ThreadPoolExecutor's keep-alive seconds.
     * <p>Default is 60.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        synchronized (this.poolSizeMonitor) {
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
            }
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    /**
     * Return the capacity for the ThreadPoolExecutor's BlockingQueue.
     *
     * @see #setQueueCapacity(int)
     * @since 5.3.21
     */
    public int getQueueCapacity() {
        return this.queueCapacity;
    }

    /**
     * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
     * <p>Default is {@code Integer.MAX_VALUE}.
     * <p>Any positive value will lead to a LinkedBlockingQueue instance;
     * any other value will lead to a SynchronousQueue instance.
     *
     * @see java.util.concurrent.LinkedBlockingQueue
     * @see java.util.concurrent.SynchronousQueue
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * Specify whether to allow core threads to time out. This enables dynamic
     * growing and shrinking even in combination with a non-zero queue (since
     * the max pool size will only grow once the queue is full).
     * <p>Default is "false".
     *
     * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
     */
    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    /**
     * Specify whether to start all core threads, causing them to idly wait for work.
     * <p>Default is "false".
     *
     * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
     * @since 5.3.14
     */
    public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
        this.prestartAllCoreThreads = prestartAllCoreThreads;
    }

    /**
     * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
     * about to be executed.
     * <p>Note that such a decorator is not necessarily being applied to the
     * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
     * execution callback (which may be a wrapper around the user-supplied task).
     * <p>The primary use case is to set some execution context around the task's
     * invocation, or to provide some monitoring/statistics for task execution.
     * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
     * is limited to plain {@code Runnable} execution via {@code execute} calls.
     * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
     * {@code FutureTask} which does not propagate any exceptions; you might
     * have to cast it and call {@code Future#get} to evaluate exceptions.
     * See the {@code ThreadPoolExecutor#afterExecute} javadoc for an example
     * of how to access exceptions in such a {@code Future} case.
     *
     * @since 4.3
     */
    public void setTaskDecorator(TaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }


    /**
     * Note: This method exposes an {@link ExecutorService} to its base class
     * but stores the actual {@link ThreadPoolExecutor} handle internally.
     * Do not override this method for replacing the executor, rather just for
     * decorating its {@code ExecutorService} handle or storing custom state.
     */
    @Override public void destroy() {
        try { super.destroy(); }
        finally { if (threadPoolExecutor != null) org.wymix.framework.tp.supports.ThreadPoolHolder.unregister(getThreadNamePrefix(), threadPoolExecutor); }
    }

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

        BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

        ExecutorServiceWrapper executor = (ExecutorServiceWrapper) ThreadPoolRegistry.registerThreadPool(this.getThreadNamePrefix(), this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS, queue, rejectedExecutionHandler);
        if (taskDecorator != null) {
            executor.setRunnableDecorator(runnable -> taskDecorator.decorate(runnable));
        }
        this.threadPoolExecutor = executor;
        if (allowCoreThreadTimeOut) {
            this.threadPoolExecutor.allowCoreThreadTimeOut(true);
        }
        if (prestartAllCoreThreads) {
            this.threadPoolExecutor.prestartAllCoreThreads(true);
        }
        return executor;
    }

    /**
     * Create the BlockingQueue to use for the ThreadPoolExecutor.
     * <p>A LinkedBlockingQueue instance will be created for a positive
     * capacity value; a SynchronousQueue else.
     *
     * @param queueCapacity the specified queue capacity
     * @return the BlockingQueue instance
     * @see java.util.concurrent.LinkedBlockingQueue
     * @see java.util.concurrent.SynchronousQueue
     */
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue<>(queueCapacity);
        } else {
            return new SynchronousQueue<>();
        }
    }

    /**
     * Return the underlying ThreadPoolExecutor for native access.
     *
     * @return the underlying ThreadPoolExecutor (never {@code null})
     * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
     */
    public ExecutorService getThreadPoolExecutor() throws IllegalStateException {
        Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
        return this.threadPoolExecutor;
    }

    /**
     * Return the current pool size.
     *
     * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
     */
    public int getPoolSize() {
        if (this.threadPoolExecutor == null) {
            // Not initialized yet: assume core pool size.
            return this.corePoolSize;
        }
        return this.threadPoolExecutor.getCorePoolSize();
    }

    /**
     * Return the current queue size.
     *
     * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
     * @since 5.3.21
     */
    public int getQueueSize() {
        if (this.threadPoolExecutor == null) {
            // Not initialized yet: assume no queued tasks.
            return 0;
        }
        return (int) this.threadPoolExecutor.standby();
    }

    /**
     * Return the number of currently active threads.
     *
     * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
     */
    public int getActiveCount() {
        if (this.threadPoolExecutor == null) {
            // Not initialized yet: assume no active threads.
            return 0;
        }
        return (int) this.threadPoolExecutor.running();
    }


    @Override
    public void execute(Runnable task) {
        Executor executor = getThreadPoolExecutor();
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
            executor.execute(future);
            return future;
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
            executor.execute(future);
            return future;
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    protected void cancelRemainingTask(Runnable task) {
        super.cancelRemainingTask(task);
        // Cancel associated user-level Future handle as well
        Object original = this.decoratedTaskMap.get(task);
        if (original instanceof Future) {
            ((Future<?>) original).cancel(true);
        }
    }

}