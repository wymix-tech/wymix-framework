package org.wymix.framework.tp.supports;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class RejectionContractTest {
    @Test
    void customSubclassOfStandardPolicyRetainsItsOverride() throws Exception {
        AtomicInteger invoked = new AtomicInteger();
        RejectedExecutionHandler policy =
                new ThreadPoolExecutor.DiscardOldestPolicy() {
                    @Override
                    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
                        invoked.incrementAndGet();
                        throw new RejectedExecutionException("custom rejection");
                    }
                };
        try (Blocked blocked = new Blocked(pool(policy))) {
            Future<?> old = blocked.pool.submit(() -> {});
            assertThrows(RejectedExecutionException.class, () -> blocked.pool.submit(() -> {}));
            assertEquals(1, invoked.get());
            assertFalse(old.isCancelled());
            blocked.finish();
            assertTrue(old.isDone());
            assertEquals(2, blocked.pool.total());
        }
    }

    @Test
    void cancellationOfRunningPeriodicTaskIsCountedOnce() throws Exception {
        ScheduledExecutorServiceWrapper pool =
                new ScheduledExecutorServiceWrapper(
                        1, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            ScheduledFuture<?> task =
                    pool.scheduleAtFixedRate(
                            () -> {
                                started.countDown();
                                try {
                                    release.await();
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            0,
                            1,
                            TimeUnit.SECONDS);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(task.cancel(true));
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(1, pool.cancelled());
            assertEquals(0, pool.remaining());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    ExecutorServiceWrapper pool(RejectedExecutionHandler handler) {
        return new ExecutorServiceWrapper(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                Executors.defaultThreadFactory(),
                handler);
    }

    static class Blocked implements AutoCloseable {
        final ExecutorServiceWrapper pool;
        final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);

        Blocked(ExecutorServiceWrapper pool) throws Exception {
            this.pool = pool;
            pool.execute(
                    () -> {
                        started.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(started.await(2, TimeUnit.SECONDS));
        }

        void finish() throws Exception {
            release.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        }

        @Override
        public void close() {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void discardOldestCancelsOldFutureAndExecutesNewTaskExactlyOnce() throws Exception {
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.DiscardOldestPolicy()))) {
            AtomicInteger calls = new AtomicInteger();
            Future<?> old = blocked.pool.submit(() -> fail("old task must be evicted"));
            Future<?> fresh = blocked.pool.submit(calls::incrementAndGet);
            assertTrue(old.isCancelled());
            assertFalse(fresh.isCancelled());
            blocked.finish();
            assertEquals(1, fresh.get());
            assertEquals(1, calls.get());
            assertEquals(3, blocked.pool.total());
            assertEquals(1, blocked.pool.cancelled());
            assertEquals(1, blocked.pool.rejected());
            assertEquals(0, blocked.pool.remaining());
            assertTrue(blocked.pool.finished());
        }
    }

    @Test
    void discardCompletesRejectedFutureWithoutGhostTasks() throws Exception {
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.DiscardPolicy()))) {
            blocked.pool.submit(() -> {});
            Future<?> discarded = blocked.pool.submit(() -> fail("discarded task ran"));
            assertTrue(discarded.isCancelled());
            blocked.finish();
            assertEquals(2, blocked.pool.total());
            assertEquals(0, blocked.pool.remaining());
            assertEquals(1, blocked.pool.rejected());
        }
    }

    @Test
    void callerRunsExecutesOnceAndRetainsSubmittedFuture() throws Exception {
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.CallerRunsPolicy()))) {
            blocked.pool.submit(() -> {});
            String caller = Thread.currentThread().getName();
            Future<String> result = blocked.pool.submit(() -> Thread.currentThread().getName());
            assertEquals(caller, result.get());
            blocked.finish();
            assertEquals(3, blocked.pool.total());
            assertEquals(1, blocked.pool.successRate());
        }
    }

    @Test
    void abortLeavesNoUnfinishedRejection() throws Exception {
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.AbortPolicy()))) {
            blocked.pool.submit(() -> {});
            assertThrows(RejectedExecutionException.class, () -> blocked.pool.submit(() -> {}));
            blocked.finish();
            assertEquals(2, blocked.pool.total());
            assertEquals(0, blocked.pool.remaining());
        }
    }

    @Test
    void customResubmissionDoesNotDoubleWrapOrCancelAcceptedTasks() throws Exception {
        RejectedExecutionHandler handler =
                (task, executor) -> {
                    Runnable oldest = executor.getQueue().poll();
                    if (oldest instanceof RunnableWrapper tracked) tracked.cancelQueued();
                    executor.execute(task);
                };
        try (Blocked blocked = new Blocked(pool(handler))) {
            Future<?> old = blocked.pool.submit(() -> {});
            Future<?> fresh = blocked.pool.submit(() -> 42);
            assertTrue(old.isCancelled());
            blocked.finish();
            assertEquals(42, fresh.get());
            assertEquals(3, blocked.pool.total());
        }
    }

    @Test
    void discardOldestWithHandoffQueueRejectsWithoutRecursion() throws Exception {
        ExecutorServiceWrapper pool =
                new ExecutorServiceWrapper(
                        1,
                        1,
                        0,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        Executors.defaultThreadFactory(),
                        new ThreadPoolExecutor.DiscardOldestPolicy());
        try (Blocked blocked = new Blocked(pool)) {
            assertThrows(RejectedExecutionException.class, () -> pool.submit(() -> {}));
            blocked.finish();
            assertEquals(1, pool.total());
        }
    }

    @Test
    void shutdownNowCancelsQueuedFuturesOnlyOnce() throws Exception {
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.AbortPolicy()))) {
            Future<?> queued = blocked.pool.submit(() -> fail("pending task ran"));
            blocked.pool.shutdownNow();
            blocked.pool.shutdownNow();
            assertTrue(queued.isCancelled());
            assertTrue(blocked.pool.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(1, blocked.pool.cancelled());
            assertEquals(0, blocked.pool.remaining());
        }
    }

    @Test
    void explicitCompletableSubmissionsReportFailuresAndCancellation() throws Exception {
        ExecutorServiceWrapper pool = pool(new ThreadPoolExecutor.AbortPolicy());
        try {
            CompletableFuture<?> failed =
                    pool.submitCompletable(
                            () -> {
                                throw new IllegalStateException("business failure");
                            });
            assertThrows(ExecutionException.class, failed::get);
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(1, pool.failed());
            assertEquals(0, pool.successRate());
            assertTrue(pool.outcomeMetricsAvailable());
        } finally {
            pool.shutdownNow();
        }
        try (Blocked blocked = new Blocked(pool(new ThreadPoolExecutor.DiscardOldestPolicy()))) {
            CompletableFuture<?> old = blocked.pool.submitCompletable(() -> {});
            CompletableFuture<?> fresh = blocked.pool.submitCompletable(() -> 42);
            assertTrue(old.isCancelled());
            blocked.finish();
            assertEquals(42, fresh.get());
        }
    }

    @Test
    void opaqueCompletableCallbacksDoNotClaimBusinessSuccess() throws Exception {
        ExecutorServiceWrapper pool = pool(new ThreadPoolExecutor.AbortPolicy());
        try {
            CompletableFuture<?> failed =
                    CompletableFuture.runAsync(
                            () -> {
                                throw new IllegalStateException();
                            },
                            pool);
            assertThrows(ExecutionException.class, failed::get);
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertFalse(pool.outcomeMetricsAvailable());
            assertEquals(0, pool.successRate());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void scheduledCompletableSubmissionsTrackFailuresAndCancelOnShutdown() throws Exception {
        ScheduledExecutorServiceWrapper pool =
                new ScheduledExecutorServiceWrapper(
                        1, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        CountDownLatch active = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            assertThrows(
                    ExecutionException.class,
                    () ->
                            pool.submitCompletable(
                                            () -> {
                                                throw new IllegalStateException();
                                            })
                                    .get());
            pool.submit(
                    () -> {
                        active.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(active.await(2, TimeUnit.SECONDS));
            CompletableFuture<?> queued = pool.submitCompletable(() -> fail("queued task ran"));
            pool.shutdownNow();
            assertTrue(queued.isCancelled());
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(1, pool.failed());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void scheduledOpaqueCallbacksMarkOutcomesUnavailable() throws Exception {
        ScheduledExecutorServiceWrapper pool =
                new ScheduledExecutorServiceWrapper(
                        1, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        try {
            assertThrows(
                    ExecutionException.class,
                    () ->
                            CompletableFuture.runAsync(
                                            () -> {
                                                throw new IllegalStateException();
                                            },
                                            pool)
                                    .get());
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertFalse(pool.outcomeMetricsAvailable());
            assertEquals(0, pool.successRate());
        } finally {
            pool.shutdownNow();
        }
    }
}
