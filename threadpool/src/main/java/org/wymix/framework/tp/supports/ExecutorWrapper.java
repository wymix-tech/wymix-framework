package org.wymix.framework.tp.supports;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public interface ExecutorWrapper extends ExecutorService {

    default int getPoolSize() {
        return getCorePoolSize();
    }

    default boolean outcomeMetricsAvailable() {
        return true;
    }

    /** Submits a user-level FutureTask so outcomes and cancellation remain observable. */
    default <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        FutureTask<T> submitted =
                new FutureTask<>(task) {
                    @Override
                    protected void done() {
                        try {
                            result.complete(get());
                        } catch (CancellationException ex) {
                            result.cancel(false);
                        } catch (ExecutionException ex) {
                            result.completeExceptionally(ex.getCause());
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            result.completeExceptionally(ex);
                        }
                    }
                };
        result.whenComplete(
                (value, error) -> {
                    if (result.isCancelled()) submitted.cancel(true);
                });
        execute(submitted);
        return result;
    }

    default CompletableFuture<Void> submitCompletable(Runnable task) {
        return submitCompletable(Executors.callable(task, null));
    }

    int getCorePoolSize();

    void setCorePoolSize(int corePoolSize);

    int getMaxPoolSize();

    void setMaxPoolSize(int maxPoolSize);

    long getKeepAliveTime();

    TimeUnit getTimeUnit();

    void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit);

    long remaining();

    long standby();

    long running();

    long total();

    long cost();

    double avgCost();

    boolean finished();

    double successRate();

    double executionRate();

    double processedRate();
}
