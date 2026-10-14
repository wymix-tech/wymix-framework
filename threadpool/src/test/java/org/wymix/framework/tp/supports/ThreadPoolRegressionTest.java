package org.wymix.framework.tp.supports;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.wymix.framework.tp.web.ThreadPoolOperateController;

import java.util.concurrent.*;

class ThreadPoolRegressionTest {
    ExecutorServiceWrapper pool(int capacity) {
        return new ExecutorServiceWrapper(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Test
    void futureExceptionsAreFailuresAndRatesAreFinite() throws Exception {
        ExecutorServiceWrapper pool = pool(3);
        try {
            assertEquals(0, pool.successRate());
            Future<?> result =
                    pool.submit(
                            () -> {
                                throw new IllegalStateException();
                            });
            assertThrows(ExecutionException.class, result::get);
            pool.shutdown();
            assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS));
            assertEquals(1, pool.failed());
            assertEquals(0, pool.successRate());
            assertEquals(1, pool.executionRate());
            assertEquals(0, pool.remaining());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rejectsDoNotRemainAsUnfinishedTasks() throws Exception {
        ExecutorServiceWrapper pool = pool(1);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            pool.execute(
                    () -> {
                        started.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            pool.execute(() -> {});
            assertThrows(RejectedExecutionException.class, () -> pool.execute(() -> {}));
            assertEquals(1, pool.rejected());
            release.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS));
            assertEquals(0, pool.remaining());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void periodicCountsExecutionsAndCancellationRemovesQueue() throws Exception {
        ScheduledExecutorServiceWrapper pool =
                new ScheduledExecutorServiceWrapper(
                        1, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        try {
            CountDownLatch ran = new CountDownLatch(4);
            ScheduledFuture<?> periodic =
                    pool.scheduleAtFixedRate(ran::countDown, 0, 5, TimeUnit.MILLISECONDS);
            assertTrue(ran.await(3, TimeUnit.SECONDS));
            periodic.cancel(false);
            Future<?> failed =
                    pool.schedule(
                            () -> {
                                throw new IllegalStateException();
                            },
                            0,
                            TimeUnit.MILLISECONDS);
            assertThrows(ExecutionException.class, failed::get);
            ScheduledFuture<?> pending =
                    pool.schedule(() -> fail("cancelled task ran"), 1, TimeUnit.DAYS);
            assertTrue(pending.cancel(false));
            pool.shutdown();
            assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS));
            assertTrue(pool.total() >= 5);
            assertTrue(pool.processedRate() <= 1);
            assertEquals(0, pool.remaining());
            assertEquals(1, pool.failed());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void resizeValidatesBeforeMutationAndOrdersExpansion() {
        String name = "resize-" + java.util.UUID.randomUUID();
        ExecutorWrapper pool =
                ThreadPoolRegistry.registerThreadPool(
                        name,
                        2,
                        4,
                        0,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        new ThreadPoolExecutor.AbortPolicy());
        try {
            new ThreadPoolOperateController()
                    .changeThreadPoolConfig(name, 8, 16, 0, TimeUnit.MILLISECONDS);
            assertEquals(8, pool.getCorePoolSize());
            assertEquals(16, pool.getMaxPoolSize());
            new ThreadPoolOperateController()
                    .changeThreadPoolConfig(name, 1, 2, 0, TimeUnit.MILLISECONDS);
            assertEquals(1, pool.getCorePoolSize());
            assertEquals(2, pool.getMaxPoolSize());
            assertThrows(IllegalArgumentException.class, () -> pool.setCorePoolSize(9));
            assertEquals(1, pool.getCorePoolSize());
        } finally {
            pool.shutdownNow();
            ThreadPoolHolder.unregister(name, pool);
        }
    }
}
