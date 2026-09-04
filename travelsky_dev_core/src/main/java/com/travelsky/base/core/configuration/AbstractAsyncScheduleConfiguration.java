package com.travelsky.base.core.configuration;

import com.travelsky.base.core.supports.ScheduledTaskThreadPoolExecutor;
import com.travelsky.base.core.supports.TaskThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;


@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties(AsyncScheduleProperties.class)
public abstract class AbstractAsyncScheduleConfiguration implements AsyncConfigurer {

    static {
        System.setProperty("spring.aop.proxy-target-class", "true");
    }

    @Autowired
    AsyncScheduleProperties asyncScheduleProperties;

    public abstract TaskDecorator taskDecorator();

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
