package org.wymix.framework.tp.supports;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/** A submitted task is wrapped once, even when a rejection policy resubmits it. */
public class RunnableWrapper implements Runnable {
    private final Runnable runnable;
    private final Future<?> result;
    private final LongAdder finished, cost, successCount, failedCount, cancelledCount;
    private final AtomicBoolean terminal = new AtomicBoolean();
    private volatile boolean started;
    private volatile boolean accepted;

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

    boolean accepted() {
        return accepted;
    }

    void accept() {
        accepted = true;
    }

    boolean discard() {
        if (started || !terminal.compareAndSet(false, true)) return false;
        if (result != null) result.cancel(false);
        return true;
    }

    void cancelQueued() {
        if (discard()) {
            cancelledCount.increment();
            finished.increment();
        }
    }

    @Override
    public void run() {
        if (terminal.get()) return;
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
            // CompletableFuture's internal callback swallows business errors.
            if (!(com.alibaba.ttl.TtlRunnable.unwrap(runnable)
                    instanceof CompletableFuture.AsynchronousCompletionTask)) {
                successCount.increment();
            }
        } catch (RuntimeException | Error ex) {
            failedCount.increment();
            throw ex;
        } finally {
            if (terminal.compareAndSet(false, true)) {
                cost.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                finished.increment();
            }
        }
    }
}
