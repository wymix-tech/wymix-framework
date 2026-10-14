package org.wymix.framework.tp.supports;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public abstract class AbstractExecutorServiceWrapper implements ExecutorWrapper {

    protected final LongAdder count = new LongAdder();
    protected final LongAdder finished = new LongAdder();
    protected final LongAdder successCount = new LongAdder();
    protected final LongAdder cost = new LongAdder();
    protected final LongAdder rejectedCount = new LongAdder();
    private final BlockingQueue<Runnable> queue;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    protected int corePoolSize;
    protected int maxPoolSize;
    protected long keepAliveTime;
    protected TimeUnit timeUnit;
    protected ExecutorService executorService;

    protected RunnableDecorator runnableDecorator;

    public AbstractExecutorServiceWrapper(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queue = queue;
        this.threadFactory = threadFactory;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    @Override
    public void execute(Runnable command) {
        this.executorService.execute(command);
    }

    /**
     * 剩余未完成任务数量
     *
     * @return
     */
    public long remaining() {
        return count.sum() - finished.sum();
    }

    /**
     * 等待执行任务数量
     *
     * @return
     */
    public long standby() {
        return queue.size();
    }

    /**
     * 正在执行任务数量
     *
     * @return
     */
    public long running() {
        return count.sum() - queue.size() - finished.sum();
    }

    /**
     * 总任务数量
     *
     * @return
     */
    public long total() {
        return count.sum();
    }

    /**
     * 总任务执行时间
     *
     * @return
     */
    public long cost() {
        return cost.sum();
    }

    /**
     * 平均任务执行时间
     *
     * @return
     */
    public double avgCost() {
        if (finished.sum() == 0) {
            return 0D;
        }
        return cost.sum() * 1.0D / finished.sum();
    }

    /**
     * 任务是否执行完成
     *
     * @return
     */
    public boolean finished() {
        return count.sum() == finished.sum();
    }

    /**
     * 任务成功率
     *
     * @return
     */
    public double successRate() {
        if (count.sum() == 0) {
            return 0D;
        }
        return successCount.sum() * 1.0D / finished.sum();
    }

    /**
     * 任务执行失败率
     *
     * @return
     */
    public double executionRate() {
        if (finished.sum() == 0) {
            return 0D;
        }
        return (finished.sum() - successCount.sum()) * 1.0D / finished.sum();
    }

    @Override
    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    @Override
    public long getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    @Override
    public int getCorePoolSize() {
        return this.corePoolSize;
    }

    public ExecutorService unwrap() {
        return this.executorService;
    }

    public double processedRate() {
        if (total() == 0) {
            return 1D;
        }
        return finished.sum() * 1.0D / total();
    }

    public void setRunnableDecorator(RunnableDecorator runnableDecorator) {
        this.runnableDecorator = runnableDecorator;
    }

    public void allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {

    }

    public void prestartAllCoreThreads(boolean prestartAllCoreThreads) {

    }
}

