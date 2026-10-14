package org.wymix.framework.core.configuration;

import org.wymix.framework.core.supports.ScheduledTaskThreadPoolExecutor;
import org.wymix.framework.core.supports.TaskThreadPoolExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 异步/调度统一自动装配。
 * 引入 core 即生效，可通过 async.enabled=false 关闭。
 *
 * <p>业务定制：
 * <ul>
 *     <li>声明 {@link TaskDecorator} Bean 即可覆盖默认装饰器（如 web 模块透传请求上下文与链路 ID）；</li>
 *     <li>声明名为 {@code taskExecutor} 的 {@link Executor} Bean 即可完全接管异步线程池。</li>
 * </ul>
 *
 * <p>注意：这里刻意<b>不</b>实现 {@code AsyncConfigurer}。Spring Boot 会为容器中的 Executor Bean
 * 自动注册 {@code applicationTaskExecutorAsyncConfigurer}，同时存在两个 AsyncConfigurer 会导致
 * “Only one AsyncConfigurer may exist”。改为直接提供 Executor Bean 由 Boot 接管。
 */
@AutoConfiguration
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties(AsyncScheduleProperties.class)
@ConditionalOnProperty(prefix = "async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncScheduleAutoConfiguration {

    static {
        System.setProperty("spring.aop.proxy-target-class", "true");
    }

    @Autowired
    AsyncScheduleProperties asyncScheduleProperties;

    @Autowired(required = false)
    ObjectProvider<TaskDecorator> taskDecoratorProvider;

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    @ConditionalOnMissingBean(name = {"applicationTaskExecutor", "taskExecutor"})
    public Executor taskExecutor() {
        TaskThreadPoolExecutor executor = new TaskThreadPoolExecutor();
        executor.setCorePoolSize(asyncScheduleProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncScheduleProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncScheduleProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(asyncScheduleProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(asyncScheduleProperties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(asyncScheduleProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(asyncScheduleProperties.getAwaitTerminationSeconds());
        executor.setTaskDecorator(taskDecoratorProvider.getIfAvailable(() -> runnable -> runnable));
        return executor;
    }

    @Bean("taskScheduler")
    @ConditionalOnMissingBean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ScheduledTaskThreadPoolExecutor scheduler = new ScheduledTaskThreadPoolExecutor();
        scheduler.setName(asyncScheduleProperties.getSchedule().getThreadNamePrefix());
        scheduler.setThreadNamePrefix(asyncScheduleProperties.getSchedule().getThreadNamePrefix());
        scheduler.setPoolSize(asyncScheduleProperties.getSchedule().getPoolSize());
        return scheduler;
    }
}
