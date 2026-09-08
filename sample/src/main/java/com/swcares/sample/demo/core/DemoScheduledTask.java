package com.swcares.sample.demo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * core 模块调度示例。
 *
 * <p>{@code @EnableScheduling} 由 core 的 {@code AsyncScheduleAutoConfiguration} 统一开启
 * （开关 {@code async.enabled}），调度线程池 Bean 名为 {@code taskScheduler}，
 * 参数见 {@code application-async.yml} 的 {@code async.schedule.*}。
 *
 * <p>这里刻意只累加计数器、不打 info 日志，避免示例应用空跑时刷屏。
 */
@Component
public class DemoScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DemoScheduledTask.class);

    private final AtomicLong ticks = new AtomicLong();

    /**
     * 每 5 分钟执行一次，首次延迟 10 秒。
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 300_000)
    public void tick() {
        long value = ticks.incrementAndGet();
        log.debug("[demo] 定时任务执行第 {} 次", value);
    }

    public long getTicks() {
        return ticks.get();
    }
}
