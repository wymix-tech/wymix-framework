package org.wymix.framework.tp.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.wymix.framework.tp.supports.*;

import java.util.concurrent.*;

class BusinessPoolConfigurationTest {
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                ThreadPoolConfiguration.class,
                                TaskExecutionAutoConfiguration.class));
    }

    @Test
    void businessDefinitionsDoNotSuppressBootAndAreDestroyedByTheContainer() {
        final ExecutorWrapper[] pool = new ExecutorWrapper[1];
        runner().withPropertyValues(
                        "thread-pool.pool.business.core-pool-size=1",
                        "thread-pool.pool.business.max-pool-size=2")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            assertInstanceOf(
                                    ThreadPoolTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                            pool[0] = ctx.getBean("business", ExecutorWrapper.class);
                            assertEquals(2, pool[0].getMaxPoolSize());
                            assertEquals(42, pool[0].submit(() -> 42).get(2, TimeUnit.SECONDS));
                            assertSame(pool[0], ThreadPoolHolder.get("business"));
                        });
        assertTrue(pool[0].isShutdown());
        assertNull(ThreadPoolHolder.get("business"));
        assertNull(ThreadPoolHolder.get("applicationTaskExecutor"));
    }

    @Test
    void zeroCapacityIsSupportedAndMaximumDefaultsToTwiceCore() {
        runner().withPropertyValues(
                        "thread-pool.pool.handoff.core-pool-size=2",
                        "thread-pool.pool.handoff.queue-capacity=0")
                .run(
                        ctx -> {
                            ExecutorWrapper pool = ctx.getBean("handoff", ExecutorWrapper.class);
                            assertEquals(4, pool.getMaxPoolSize());
                            assertEquals(0, pool.standby());
                        });
    }

    @Test
    void reservedBeanNamesFailBeforePoolsAreCreated() {
        for (String name :
                new String[] {
                    "taskExecutor", "applicationTaskExecutor", "taskScheduler", "bootstrapExecutor"
                }) {
            runner().withPropertyValues("thread-pool.pool." + name + ".core-pool-size=1")
                    .run(ctx -> assertNotNull(ctx.getStartupFailure()));
        }
    }

    @Test
    void invalidBoundsAndRejectionClassesFailEarly() {
        runner().withPropertyValues(
                        "thread-pool.pool.invalid.core-pool-size=3",
                        "thread-pool.pool.invalid.max-pool-size=2")
                .run(ctx -> assertNotNull(ctx.getStartupFailure()));
        runner().withPropertyValues(
                        "thread-pool.pool.invalid.rejected-policy=CUSTOM_POLICY",
                        "thread-pool.pool.invalid.rejected-policy-class-name=java.lang.String")
                .run(ctx -> assertNotNull(ctx.getStartupFailure()));
        assertNull(ThreadPoolHolder.get("invalid"));
    }

    @Test
    void moduleDisableDoesNotAlterBootOwnership() {
        runner().withPropertyValues(
                        "wymix.threadpool.enabled=false",
                        "thread-pool.pool.business.core-pool-size=1")
                .run(
                        ctx -> {
                            assertFalse(ctx.containsBean("business"));
                            assertInstanceOf(
                                    ThreadPoolTaskExecutor.class,
                                    ctx.getBean("applicationTaskExecutor"));
                            assertNull(ThreadPoolHolder.get("applicationTaskExecutor"));
                        });
    }
}
