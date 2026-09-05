package com.travelsky.base.core.configuration;

import com.travelsky.base.core.supports.ScheduledTaskThreadPoolExecutor;
import com.travelsky.base.core.supports.TaskThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 异步/调度统一自动装配。
 * 引入 travelsky_dev_core 即生效，可通过 async.enabled=false 关闭。
 * 业务定制：声明 TaskDecorator Bean 即可覆盖默认装饰器（如透传请求上下文）。
 */
@AutoConfiguration
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties(AsyncScheduleProperties.class)
@ConditionalOnProperty(prefix = "async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncScheduleAutoConfiguration implements AsyncConfigurer {

    static {
        System.setProperty("spring.aop.proxy-target-class", "true");
    }

    @Autowired
    AsyncScheduleProperties asyncScheduleProperties;

    @Autowired(required = false)
    ObjectProvider<TaskDecorator> taskDecoratorProvider;

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator taskDecorator() {
        return runnable -> runnable;
    }

    @Bean
    @Override
    public Executor getAsyncExecutor() {
        TaskThreadPoolExecutor executor = new TaskThreadPoolExecutor();
        executor.setCorePoolSize(asyncScheduleProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncScheduleProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncScheduleProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(asyncScheduleProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(asyncScheduleProperties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(asyncScheduleProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(asyncScheduleProperties.getAwaitTerminationSeconds());
        executor.setTaskDecorator(taskDecorator());
        return executor;
    }

    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ScheduledTaskThreadPoolExecutor scheduler = new ScheduledTaskThreadPoolExecutor();
        scheduler.setName(asyncScheduleProperties.getSchedule().getThreadNamePrefix());
        scheduler.setThreadNamePrefix(asyncScheduleProperties.getSchedule().getThreadNamePrefix());
        scheduler.setPoolSize(asyncScheduleProperties.getSchedule().getPoolSize());
        return scheduler;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
