package org.wymix.framework.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.task.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.*;
import org.wymix.framework.core.supports.*;
import org.wymix.framework.tp.configuration.ThreadPoolConfiguration;
import org.wymix.framework.tp.supports.*;

import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class BootTaskContractTest {
    @Test
    void compatibilityPoolAlreadyRegisteredUnderBeanNameIsNotRegisteredTwice() {
        runner().withUserConfiguration(LegacyScheduler.class)
                .withPropertyValues("spring.task.execution.mode=force")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            assertNotNull(ThreadPoolHolder.get("compatibilityScheduler"));
                            assertFalse(
                                    ctx.getBean(
                                                    "compatibilityScheduler",
                                                    ScheduledTaskThreadPoolExecutor.class)
                                            .getScheduledExecutor()
                                            .isShutdown());
                        });
        assertNull(ThreadPoolHolder.get("compatibilityScheduler"));
    }

    @Configuration(proxyBeanMethods = false)
    static class LegacyScheduler {
        @Bean
        ScheduledTaskThreadPoolExecutor compatibilityScheduler() {
            ScheduledTaskThreadPoolExecutor scheduler = new ScheduledTaskThreadPoolExecutor();
            scheduler.setName("compatibilityScheduler");
            return scheduler;
        }
    }

    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                AsyncScheduleAutoConfiguration.class, ThreadPoolConfiguration.class,
                                TaskExecutionAutoConfiguration.class,
                                        TaskSchedulingAutoConfiguration.class));
    }

    @Test
    void defaultsAreNativeAndObservedWithoutReplacingTheBean() {
        runner().run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            ThreadPoolTaskExecutor executor =
                                    ctx.getBean(
                                            "applicationTaskExecutor",
                                            ThreadPoolTaskExecutor.class);
                            assertEquals(ThreadPoolTaskExecutor.class, executor.getClass());
                            assertEquals(0, executor.getPoolSize());
                            assertEquals(8, executor.getCorePoolSize());
                            assertNotNull(ThreadPoolHolder.get("applicationTaskExecutor"));
                            assertFalse(
                                    ThreadPoolMonitor.getThreadPoolContext(
                                                    "applicationTaskExecutor")
                                            .isOutcomeMetricsAvailable());
                            assertEquals(
                                    0,
                                    ThreadPoolMonitor.getThreadPoolContext(
                                                    "applicationTaskExecutor")
                                            .getPoolSize());
                            assertEquals(
                                    ThreadPoolTaskScheduler.class,
                                    ctx.getBean("taskScheduler").getClass());
                        });
        assertNull(ThreadPoolHolder.get("applicationTaskExecutor"));
        assertNull(ThreadPoolHolder.get("taskScheduler"));
    }

    @Test
    void explicitLegacySettingsStillCustomizeTheRealPool() {
        runner().withPropertyValues(
                        "async.corePoolSize=2",
                        "async.maxPoolSize=4",
                        "async.queueCapacity=3",
                        "async.keepAliveSeconds=12",
                        "async.threadNamePrefix=legacy-",
                        "async.schedule.poolSize=2")
                .run(
                        ctx -> {
                            ThreadPoolTaskExecutor executor =
                                    ctx.getBean(
                                            "applicationTaskExecutor",
                                            ThreadPoolTaskExecutor.class);
                            assertEquals(2, executor.getCorePoolSize());
                            assertEquals(4, executor.getMaxPoolSize());
                            assertEquals(3, executor.getQueueCapacity());
                            assertEquals(12, executor.getKeepAliveSeconds());
                            assertEquals("legacy-", executor.getThreadNamePrefix());
                            assertEquals(
                                    2,
                                    ctx.getBean("taskScheduler", ThreadPoolTaskScheduler.class)
                                            .getScheduledThreadPoolExecutor()
                                            .getCorePoolSize());
                        });
    }

    @Test
    void canonicalSettingsTakePrecedenceIncludingDurationValues() {
        runner().withPropertyValues(
                        "async.core-pool-size=3",
                        "async.keep-alive-seconds=99",
                        "spring.task.execution.pool.core-size=2",
                        "spring.task.execution.pool.keep-alive=7s",
                        "async.schedule.pool-size=4",
                        "spring.task.scheduling.pool.size=2")
                .run(
                        ctx -> {
                            ThreadPoolTaskExecutor executor =
                                    ctx.getBean(
                                            "applicationTaskExecutor",
                                            ThreadPoolTaskExecutor.class);
                            assertEquals(2, executor.getCorePoolSize());
                            assertEquals(7, executor.getKeepAliveSeconds());
                            assertEquals(
                                    2,
                                    ctx.getBean("taskScheduler", ThreadPoolTaskScheduler.class)
                                            .getScheduledThreadPoolExecutor()
                                            .getCorePoolSize());
                        });
    }

    @Test
    void forceWorksWhenFrameworkConfigurationIsImportedEarly() {
        new ApplicationContextRunner()
                .withUserConfiguration(AsyncScheduleAutoConfiguration.class)
                .withConfiguration(
                        AutoConfigurations.of(
                                TaskExecutionAutoConfiguration.class,
                                TaskSchedulingAutoConfiguration.class))
                .withPropertyValues("spring.task.execution.mode=force")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            assertInstanceOf(
                                    AsyncTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                            assertEquals(1, ctx.getBeansOfType(AsyncConfigurer.class).size());
                        });
    }

    @Test
    void customNamedTaskExecutorAndForceFollowBootSelection() {
        runner().withUserConfiguration(CustomTasks.class)
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            assertFalse(ctx.containsBean("applicationTaskExecutor"));
                            assertTrue(
                                    ctx.getBean(AsyncWork.class)
                                            .thread()
                                            .get(2, TimeUnit.SECONDS)
                                            .startsWith("user-"));
                        });
        runner().withUserConfiguration(CustomTasks.class)
                .withPropertyValues("spring.task.execution.mode=force")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            assertInstanceOf(
                                    AsyncTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                            assertTrue(
                                    ctx.getBean(AsyncWork.class)
                                            .thread()
                                            .get(2, TimeUnit.SECONDS)
                                            .startsWith("task-"));
                        });
    }

    @Test
    void userAsyncConfigurerWinsEvenInForceMode() {
        runner().withUserConfiguration(CustomConfigurer.class)
                .withPropertyValues("spring.task.execution.mode=force")
                .run(
                        ctx -> {
                            assertEquals(1, ctx.getBeansOfType(AsyncConfigurer.class).size());
                            assertEquals(
                                    Thread.currentThread().getName(),
                                    ctx.getBean(AsyncWork.class).thread().get());
                            assertInstanceOf(
                                    AsyncTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                        });
    }

    @Test
    void userSchedulerWithAnotherNamePreventsAdditionalScheduler() {
        runner().withUserConfiguration(UserScheduler.class)
                .withPropertyValues("spring.task.execution.mode=force")
                .run(
                        ctx -> {
                            assertFalse(ctx.containsBean("taskScheduler"));
                            assertEquals(
                                    1,
                                    ctx.getBeansOfType(
                                                    org.springframework.scheduling.TaskScheduler
                                                            .class)
                                            .size());
                            assertSame(
                                    ctx.getBean("customScheduler"),
                                    ctx.getBean(
                                            org.springframework.scheduling.TaskScheduler.class));
                        });
    }

    @Test
    void taskDecoratorsComposeAndAlsoDecorateScheduledTasks() {
        runner().withUserConfiguration(Decorators.class)
                .run(
                        ctx -> {
                            AtomicInteger calls = ctx.getBean(AtomicInteger.class);
                            ThreadPoolTaskExecutor executor =
                                    ctx.getBean(
                                            "applicationTaskExecutor",
                                            ThreadPoolTaskExecutor.class);
                            executor.submit(() -> {}).get(2, TimeUnit.SECONDS);
                            ThreadPoolTaskScheduler scheduler =
                                    ctx.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
                            scheduler.schedule(() -> {}, Instant.now()).get(2, TimeUnit.SECONDS);
                            assertEquals(4, calls.get());
                        });
    }

    @Test
    void nativePauseAndCoordinatedStopRetainTheirContracts() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        CountDownLatch ran = new CountDownLatch(1),
                active = new CountDownLatch(1),
                release = new CountDownLatch(1),
                stopped = new CountDownLatch(1);
        try {
            executor.stop();
            executor.execute(ran::countDown);
            assertFalse(ran.await(100, TimeUnit.MILLISECONDS));
            executor.start();
            assertTrue(ran.await(2, TimeUnit.SECONDS));
            executor.execute(
                    () -> {
                        active.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(active.await(2, TimeUnit.SECONDS));
            executor.stop(stopped::countDown);
            assertFalse(stopped.await(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(stopped.await(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.start();
            executor.destroy();
        }
    }

    @Test
    void schedulerResizeAndShutdownPoliciesReachTheNativePool() {
        runner().run(
                        ctx -> {
                            ThreadPoolTaskScheduler scheduler =
                                    ctx.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
                            new org.wymix.framework.tp.web.ThreadPoolOperateController()
                                    .changeThreadPoolConfig(
                                            "taskScheduler", 3, 3, -1, TimeUnit.SECONDS);
                            assertEquals(
                                    3,
                                    scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
                            scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                            ScheduledFuture<?> delayed =
                                    scheduler.schedule(
                                            () -> fail("cancelled task executed"),
                                            Instant.now().plusSeconds(3600));
                            scheduler.getScheduledExecutor().shutdown();
                            assertTrue(delayed.isCancelled());
                        });
    }

    @Test
    void compatibilityClassesRetainNativeApisAndThreadFactory() throws Exception {
        TaskThreadPoolExecutor executor = new TaskThreadPoolExecutor();
        executor.setThreadNamePrefix("compat-async-");
        executor.setCorePoolSize(3);
        executor.setDaemon(true);
        executor.setThreadFactory(
                task -> {
                    Thread thread = new Thread(task, "chosen-factory");
                    thread.setDaemon(true);
                    return thread;
                });
        executor.initialize();
        try {
            assertEquals(0, executor.getPoolSize());
            assertEquals(
                    "chosen-factory",
                    executor.submit(() -> Thread.currentThread().getName()).get());
            assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get());
            assertThrows(
                    ExecutionException.class,
                    () ->
                            executor.submitCompletable(
                                            () -> {
                                                throw new IllegalStateException();
                                            })
                                    .get());
            assertFalse(ThreadPoolHolder.get("compat-async-").outcomeMetricsAvailable());
        } finally {
            executor.destroy();
        }
        assertNull(ThreadPoolHolder.get("compat-async-"));
        ScheduledTaskThreadPoolExecutor scheduler = new ScheduledTaskThreadPoolExecutor();
        scheduler.setName("compat-scheduler");
        scheduler.setPoolSize(1);
        AtomicInteger calls = new AtomicInteger();
        scheduler.setTaskDecorator(
                r ->
                        () -> {
                            calls.incrementAndGet();
                            r.run();
                        });
        scheduler.initialize();
        try {
            scheduler.schedule(() -> {}, Instant.now()).get();
            assertEquals(1, calls.get());
            scheduler.setPoolSize(3);
            assertEquals(3, scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
        } finally {
            scheduler.destroy();
        }
        assertNull(ThreadPoolHolder.get("compat-scheduler"));
    }

    @Test
    void virtualThreadsRetainBootNativeSelectionOnJava21() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.version().feature() >= 21);
        runner().withUserConfiguration(Decorators.class)
                .withPropertyValues("spring.threads.virtual.enabled=true")
                .run(
                        ctx -> {
                            assertInstanceOf(
                                    org.springframework.core.task.SimpleAsyncTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                            assertInstanceOf(
                                    org.springframework.scheduling.concurrent
                                            .SimpleAsyncTaskScheduler.class,
                                    ctx.getBean("taskScheduler"));
                            boolean virtual =
                                    ctx.getBean("applicationTaskExecutor", AsyncTaskExecutor.class)
                                            .submit(
                                                    () ->
                                                            (Boolean)
                                                                    Thread.class
                                                                            .getMethod("isVirtual")
                                                                            .invoke(
                                                                                    Thread
                                                                                            .currentThread()))
                                            .get(2, TimeUnit.SECONDS);
                            assertTrue(virtual);
                            assertEquals(2, ctx.getBean(AtomicInteger.class).get());
                            assertNull(ThreadPoolHolder.get("applicationTaskExecutor"));
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTasks {
        @Bean
        ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
            pool.setThreadNamePrefix("user-");
            return pool;
        }

        @Bean
        AsyncWork asyncWork() {
            return new AsyncWork();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomConfigurer {
        @Bean
        AsyncConfigurer asyncConfigurer() {
            return new AsyncConfigurer() {
                @Override
                public Executor getAsyncExecutor() {
                    return Runnable::run;
                }
            };
        }

        @Bean
        AsyncWork asyncWork() {
            return new AsyncWork();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserScheduler {
        @Bean
        ThreadPoolTaskScheduler customScheduler() {
            return new ThreadPoolTaskScheduler();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Decorators {
        @Bean
        AtomicInteger calls() {
            return new AtomicInteger();
        }

        @Bean
        TaskDecorator first(AtomicInteger calls) {
            return r ->
                    () -> {
                        calls.incrementAndGet();
                        r.run();
                    };
        }

        @Bean
        TaskDecorator second(AtomicInteger calls) {
            return r ->
                    () -> {
                        calls.incrementAndGet();
                        r.run();
                    };
        }
    }

    static class AsyncWork {
        @Async
        public CompletableFuture<String> thread() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }
}
