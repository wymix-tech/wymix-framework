package org.wymix.framework.tp.supports;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class RunnableWrapper implements Runnable {
    private final Runnable runnable;
    private final Future<?> result;
    private final LongAdder finished, cost, successCount, failedCount, cancelledCount;
    private volatile boolean started;

    public RunnableWrapper(
            Runnable runnable, LongAdder finished, LongAdder cost, LongAdder successCount) {
        this(
                runnable,
                runnable instanceof Future<?> future ? future : null,
                finished,
                cost,
                successCount,
                new LongAdder(),
                new LongAdder());
    }

    public RunnableWrapper(
            Runnable runnable,
            Future<?> result,
            LongAdder finished,
            LongAdder cost,
            LongAdder successCount,
            LongAdder failedCount,
            LongAdder cancelledCount) {
        this.runnable = runnable;
        this.result = result;
        this.finished = finished;
        this.cost = cost;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.cancelledCount = cancelledCount;
    }

    boolean started() {
        return started;
    }

    void discard() {
        if (result != null) result.cancel(false);
    }

    @Override
    public void run() {
        started = true;
        long start = System.nanoTime();
        try {
            runnable.run();
            if (result != null && result.isDone()) {
                try {
                    result.get();
                } catch (CancellationException ex) {
                    cancelledCount.increment();
                    return;
                } catch (ExecutionException ex) {
                    failedCount.increment();
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    failedCount.increment();
                    return;
                }
            }
            successCount.increment();
        } catch (RuntimeException | Error ex) {
            failedCount.increment();
            throw ex;
        } finally {
            cost.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            finished.increment();
        }
    }
}
