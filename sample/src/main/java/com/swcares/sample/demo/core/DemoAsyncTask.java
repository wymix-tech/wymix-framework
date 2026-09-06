package com.swcares.sample.demo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * core 模块异步/调度示例。
 *
 * <p>core 的 {@code AsyncScheduleAutoConfiguration} 已通过 {@code @EnableAsync}、{@code @EnableScheduling}
 * 注册好线程池，线程池参数见 {@code application-async.yml}（前缀 {@code async}）。
 */
@Component
public class DemoAsyncTask {

    private static final Logger log = LoggerFactory.getLogger(DemoAsyncTask.class);

    /**
     * 异步执行：需由外部 Bean 调用（同类内部调用不会被 AOP 增强）。
     */
    @Async
    public CompletableFuture<String> asyncTask(String param) {
        log.info("[demo] 异步任务执行中, thread={}, param={}", Thread.currentThread().getName(), param);
        return CompletableFuture.completedFuture("async:" + param);
    }
}
