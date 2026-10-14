package com.swcares.sample.demo.threadpool;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wymix.framework.tp.supports.ExecutorWrapper;
import org.wymix.framework.tp.supports.ThreadPoolHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * threadpool 示例。
 *
 * <p>threadpool 模块会读取 {@code thread-pool.pool.*} / {@code thread-pool.scheduled.*} （见 {@code
 * application-async.yml}），并以配置项名为 BeanName 自动注册线程池。 注入时通过 {@code @Qualifier} 指定 BeanName，类型统一为
 * {@link ExecutorWrapper}， 支持运行期调整核心/最大线程数并观测运行状态。
 */
@RestController
@RequestMapping("demo/threadpool")
@Tag(name = "threadpool 模块示例")
public class ThreadPoolDemoController {

    private final ExecutorWrapper testThreadPool;
    private final ExecutorWrapper testScheduledThreadPool;

    public ThreadPoolDemoController(
            @Qualifier("testThreadPool") ExecutorWrapper testThreadPool,
            @Qualifier("testScheduledThreadPool") ExecutorWrapper testScheduledThreadPool) {
        this.testThreadPool = testThreadPool;
        this.testScheduledThreadPool = testScheduledThreadPool;
    }

    @PostMapping("submit")
    @Operation(summary = "向自定义线程池提交任务")
    public String submit(@RequestParam(defaultValue = "sample") String param) throws Exception {
        CompletableFuture<String> future = testThreadPool.submitCompletable(() -> "done:" + param);
        return future.get();
    }

    @PostMapping("schedule")
    @Operation(summary = "向自定义调度线程池提交任务")
    public String schedule(@RequestParam(defaultValue = "sample") String param) throws Exception {
        // 调度池同样以 ExecutorWrapper 暴露；此处按其 ExecutorService 接口提交普通任务，
        // 延迟/周期调度由 Spring 的 @Scheduled（走 core 的 taskScheduler）承担
        CompletableFuture<String> future =
                testScheduledThreadPool.submitCompletable(() -> "scheduled:" + param);
        return future.get();
    }

    @GetMapping("status")
    @Operation(summary = "查看自定义线程池运行状态")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("poolSize", testThreadPool.getPoolSize());
        status.put("outcomeMetricsAvailable", testThreadPool.outcomeMetricsAvailable());
        status.put("corePoolSize", testThreadPool.getCorePoolSize());
        status.put("maxPoolSize", testThreadPool.getMaxPoolSize());
        status.put("keepAliveTime", testThreadPool.getKeepAliveTime());
        status.put("timeUnit", testThreadPool.getTimeUnit().name());
        status.put("running", testThreadPool.running());
        status.put("total", testThreadPool.total());
        status.put("remaining", testThreadPool.remaining());
        status.put("standby", testThreadPool.standby());
        status.put("cost", testThreadPool.cost());
        status.put("avgCost", testThreadPool.avgCost());
        status.put("finished", testThreadPool.finished());
        status.put("successRate", testThreadPool.successRate());
        status.put("executionRate", testThreadPool.executionRate());
        status.put("processedRate", testThreadPool.processedRate());
        return status;
    }

    @PutMapping("resize")
    @Operation(summary = "运行期调整核心/最大线程数（ExecutorWrapper 支持动态调参）")
    public Map<String, Object> resize(
            @RequestParam int corePoolSize, @RequestParam int maxPoolSize) {
        new org.wymix.framework.tp.web.ThreadPoolOperateController()
                .changeThreadPoolConfig(
                        "testThreadPool",
                        corePoolSize,
                        maxPoolSize,
                        -1,
                        java.util.concurrent.TimeUnit.SECONDS);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("corePoolSize", testThreadPool.getCorePoolSize());
        result.put("maxPoolSize", testThreadPool.getMaxPoolSize());
        return result;
    }

    @GetMapping("names")
    @Operation(summary = "列出全部已注册线程池名称")
    public Iterable<String> names() {
        // ThreadPoolHolder 持有所有由 threadpool 模块注册的线程池
        return ThreadPoolHolder.threadPoolNames();
    }

    @GetMapping("scheduled/status")
    @Operation(summary = "查看自定义调度线程池状态")
    public Map<String, Object> scheduledStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("corePoolSize", testScheduledThreadPool.getCorePoolSize());
        status.put("maxPoolSize", testScheduledThreadPool.getMaxPoolSize());
        status.put("total", testScheduledThreadPool.total());
        status.put("running", testScheduledThreadPool.running());
        return status;
    }
}
