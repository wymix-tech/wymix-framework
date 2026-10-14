package org.wymix.framework.core.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.task.SimpleAsyncTaskExecutorCustomizer;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.function.Consumer;

/**
 * Enables async/scheduled annotations and customizes Boot's native executors. Explicit
 * spring.task.* settings take precedence over legacy async.* settings. Boot owns executor
 * selection, force mode, virtual threads and user overrides.
 */
@AutoConfiguration(
        before = {TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class})
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "async",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AsyncScheduleAutoConfiguration {

    @Bean
    ThreadPoolTaskExecutorCustomizer legacyAsyncCustomizer(
            Environment environment, ObjectProvider<TaskDecorator> decorators) {
        return executor -> {
            Binder binder = Binder.get(environment);
            legacy(
                    binder,
                    "spring.task.execution.pool.core-size",
                    "async.core-pool-size",
                    Integer.class,
                    executor::setCorePoolSize);
            legacy(
                    binder,
                    "spring.task.execution.pool.max-size",
                    "async.max-pool-size",
                    Integer.class,
                    executor::setMaxPoolSize);
            legacy(
                    binder,
                    "spring.task.execution.pool.queue-capacity",
                    "async.queue-capacity",
                    Integer.class,
                    executor::setQueueCapacity);
            legacy(
                    binder,
                    "spring.task.execution.pool.keep-alive",
                    "async.keep-alive-seconds",
                    Integer.class,
                    executor::setKeepAliveSeconds);
            legacy(
                    binder,
                    "spring.task.execution.thread-name-prefix",
                    "async.thread-name-prefix",
                    String.class,
                    executor::setThreadNamePrefix);
            legacy(
                    binder,
                    "spring.task.execution.shutdown.await-termination",
                    "async.wait-for-tasks-to-complete-on-shutdown",
                    Boolean.class,
                    executor::setWaitForTasksToCompleteOnShutdown);
            legacy(
                    binder,
                    "spring.task.execution.shutdown.await-termination-period",
                    "async.await-termination-seconds",
                    Integer.class,
                    executor::setAwaitTerminationSeconds);
            var ordered = decorators.orderedStream().toList();
            if (!ordered.isEmpty()) executor.setTaskDecorator(new CompositeTaskDecorator(ordered));
        };
    }

    @Bean
    ThreadPoolTaskSchedulerCustomizer legacyScheduleCustomizer(
            Environment environment, ObjectProvider<TaskDecorator> decorators) {
        return scheduler -> {
            Binder binder = Binder.get(environment);
            legacy(
                    binder,
                    "spring.task.scheduling.pool.size",
                    "async.schedule.pool-size",
                    Integer.class,
                    scheduler::setPoolSize);
            legacy(
                    binder,
                    "spring.task.scheduling.thread-name-prefix",
                    "async.schedule.thread-name-prefix",
                    String.class,
                    scheduler::setThreadNamePrefix);
            var ordered = decorators.orderedStream().toList();
            if (!ordered.isEmpty()) scheduler.setTaskDecorator(new CompositeTaskDecorator(ordered));
        };
    }

    @Bean
    SimpleAsyncTaskExecutorCustomizer legacySimpleAsyncCustomizer(
            Environment environment, ObjectProvider<TaskDecorator> decorators) {
        return executor -> {
            legacy(
                    Binder.get(environment),
                    "spring.task.execution.thread-name-prefix",
                    "async.thread-name-prefix",
                    String.class,
                    executor::setThreadNamePrefix);
            var ordered = decorators.orderedStream().toList();
            if (!ordered.isEmpty()) executor.setTaskDecorator(new CompositeTaskDecorator(ordered));
        };
    }

    @Bean
    SimpleAsyncTaskSchedulerCustomizer legacySimpleScheduleCustomizer(
            Environment environment, ObjectProvider<TaskDecorator> decorators) {
        return scheduler -> {
            legacy(
                    Binder.get(environment),
                    "spring.task.scheduling.thread-name-prefix",
                    "async.schedule.thread-name-prefix",
                    String.class,
                    scheduler::setThreadNamePrefix);
            var ordered = decorators.orderedStream().toList();
            if (!ordered.isEmpty()) scheduler.setTaskDecorator(new CompositeTaskDecorator(ordered));
        };
    }

    private static <T> void legacy(
            Binder binder, String canonical, String oldName, Class<T> type, Consumer<T> apply) {
        // Presence is checked as a String so canonical Duration and boolean formats are supported.
        if (!binder.bind(canonical, String.class).isBound())
            binder.bind(oldName, type).ifBound(apply);
    }
}
