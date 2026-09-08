package com.swcares.sample.demo.core;

import com.swcares.sample.service.SampleUserService;
import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.core.dto.Message;
import com.travelsky.base.core.message.MessageSenderService;
import com.travelsky.base.core.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * travelsky_dev_core 示例：应用上下文持有、消息门面 + SPI、异步执行、定时任务。
 */
@RestController
@RequestMapping("demo/core")
@Tag(name = "core 模块示例")
public class CoreDemoController {

    private final MessageService messageService;
    private final DemoAsyncTask demoAsyncTask;
    private final DemoScheduledTask demoScheduledTask;

    public CoreDemoController(MessageService messageService,
                              DemoAsyncTask demoAsyncTask,
                              DemoScheduledTask demoScheduledTask) {
        this.messageService = messageService;
        this.demoAsyncTask = demoAsyncTask;
        this.demoScheduledTask = demoScheduledTask;
    }

    @GetMapping("contextHolder")
    @Operation(summary = "ApplicationContextHolder 静态获取 Bean / 按类型取全部实现")
    public Map<String, Object> contextHolder() {
        // 在非 Spring 管理的类（如工具类、静态回调）中也能拿到容器里的 Bean
        SampleUserService service = ApplicationContextHolder.getBean(SampleUserService.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byType", service.getClass().getSimpleName());
        // getBeansOfType 常用于「收集全部 SPI 实现」，如消息发送器的路由就是这么做的
        result.put("messageSendersByType",
                ApplicationContextHolder.getBeansOfType(MessageSenderService.class).keySet());
        return result;
    }

    @PostMapping("message")
    @Operation(summary = "MessageService 消息门面发送（按 SendType 路由到 MessageSenderService）")
    public String sendMessage(@RequestParam(defaultValue = "hello sample") String content) {
        // of(...) 会生成雪花算法消息 ID；send(...) 按 SendType 分发给对应 MessageSenderService
        Message message = messageService.of(Message.SendType.P);
        message.setSubject("sample 消息示例");
        message.setContent(content);
        message.setTo(Set.of("admin"));
        messageService.send(message);
        return message.getId();
    }

    @GetMapping("async")
    @Operation(summary = "@Async 异步任务（线程池参数见 application-async.yml）")
    public String async(@RequestParam(defaultValue = "test") String param) throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = demoAsyncTask.asyncTask(param);
        return future.get();
    }

    @GetMapping("scheduled")
    @Operation(summary = "@Scheduled 定时任务（@EnableScheduling 由 core 自动开启）")
    public Map<String, Object> scheduled() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticks", demoScheduledTask.getTicks());
        result.put("note", "DemoScheduledTask 每 5 分钟累加一次，启动 10 秒后首次触发");
        return result;
    }
}
