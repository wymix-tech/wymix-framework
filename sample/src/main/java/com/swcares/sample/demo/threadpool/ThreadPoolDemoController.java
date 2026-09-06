package com.swcares.sample.demo.threadpool;

import com.travelsky.base.tp.supports.ExecutorWrapper;
import com.travelsky.base.tp.supports.ThreadPoolHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * travelsky_dev_threadpool 示例。
 *
 * <p>threadpool 模块会读取 {@code thread-pool.pool.*} / {@code thread-pool.scheduled.*}
 * （见 {@code application-async.yml}），并以配置项名为 BeanName 自动注册线程池。
 * 注入时通过 {@code @Qualifier} 指定 BeanName，类型统一为 {@link ExecutorWrapper}，
 * 支持运行期调整核心/最大线程数并观测运行状态。
 */
@RestController
@RequestMapping("demo/threadpool")
@Tag(name = "threadpool 模块示例")
public class ThreadPoolDemoController {

    private final ExecutorWrapper testThreadPool;

    public ThreadPoolDemoController(@Qualifier("testThreadPool") ExecutorWrapper testThreadPool) {
        this.testThreadPool = testThreadPool;
    }

    @PostMapping("submit")
    @Operation(summary = "向自定义线程池提交任务")
    public String submit(@RequestParam(defaultValue = "sample") String param) throws Exception {
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> "done:" + param, testThreadPool);
        return future.get();
    }

    @GetMapping("status")
    @Operation(summary = "查看自定义线程池运行状态")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("corePoolSize", testThreadPool.getCorePoolSize());
        status.put("maxPoolSize", testThreadPool.getMaxPoolSize());
        status.put("running", testThreadPool.running());
        status.put("total", testThreadPool.total());
        status.put("remaining", testThreadPool.remaining());
        status.put("standby", testThreadPool.standby());
        return status;
    }

    @GetMapping("names")
    @Operation(summary = "列出全部已注册线程池名称")
    public Iterable<String> names() {
        // ThreadPoolHolder 持有所有由 threadpool 模块注册的线程池
        return ThreadPoolHolder.threadPoolNames();
    }
}
