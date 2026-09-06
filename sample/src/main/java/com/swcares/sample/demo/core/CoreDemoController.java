package com.swcares.sample.demo.core;

import com.swcares.sample.service.SampleUserService;
import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.core.dto.Message;
import com.travelsky.base.core.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * travelsky_dev_core 示例：应用上下文持有、消息门面、异步执行。
 */
@RestController
@RequestMapping("demo/core")
@Tag(name = "core 模块示例")
public class CoreDemoController {

    private final MessageService messageService;
    private final DemoAsyncTask demoAsyncTask;

    public CoreDemoController(MessageService messageService, DemoAsyncTask demoAsyncTask) {
        this.messageService = messageService;
        this.demoAsyncTask = demoAsyncTask;
    }

    @GetMapping("contextHolder")
    @Operation(summary = "ApplicationContextHolder 静态获取 Bean")
    public String contextHolder() {
        // 在非 Spring 管理的类（如工具类、静态回调）中也能拿到容器里的 Bean
        SampleUserService service = ApplicationContextHolder.getBean(SampleUserService.class);
        return service.getClass().getSimpleName();
    }

    @PostMapping("message")
    @Operation(summary = "MessageService 消息门面发送")
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
}
