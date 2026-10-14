package com.swcares.sample.demo.web;

import org.wymix.framework.web.aop.RepeatRequest;
import org.wymix.framework.web.context.RequestContextHolder;
import org.wymix.framework.web.i18n.MessageResourceHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * web 示例：统一响应包装、i18n、链路 TraceId、参数校验、慢请求、防重提交。
 *
 * <p>web 模块的 {@code ControllerResponseAdvice} 会把 Controller 返回值统一包装为
 * {@code ResultVO}，因此这里直接返回业务对象即可。
 */
@RestController
@RequestMapping("demo/web")
@Tag(name = "web 模块示例")
public class WebDemoController {

    @GetMapping("payload")
    @Operation(summary = "返回业务对象，由 ControllerResponseAdvice 自动包装为 ResultVO")
    public Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("module", "web");
        payload.put("advice", "ControllerResponseAdvice");
        return payload;
    }

    @GetMapping("i18n")
    @Operation(summary = "MessageResourceHelper 国际化：随 Accept-Language 返回不同文案")
    public Map<String, Object> i18n() {
        Locale locale = MessageResourceHelper.resolveLocale();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("locale", locale.toString());
        result.put("isEnglish", MessageResourceHelper.isEnLanguage());
        // code 取自 src/main/resources/messages*.properties
        result.put("greeting", MessageResourceHelper.generateMessages("demo.greeting"));
        // 带占位符的文案：messages.properties 中未定义该 code 时原样返回 code
        result.put("withArgs", MessageResourceHelper.generateMessages("demo.greeting", new Object[]{"arg"}));
        return result;
    }

    @GetMapping("requestContext")
    @Operation(summary = "RequestContextHolder 静态获取请求，以及 TraceId 过滤器注入的链路 ID")
    public Map<String, Object> requestContext() {
        Map<String, Object> result = new LinkedHashMap<>();
        var request = RequestContextHolder.getRequest();
        result.put("uri", request == null ? null : request.getRequestURI());
        result.put("method", request == null ? null : request.getMethod());
        // web 模块的 TraceId 过滤器把链路 ID 写入 log4j2 的 ThreadContext
        result.put("customTraceId", ThreadContext.get("customTraceId"));
        return result;
    }

    @PostMapping("validate")
    @Operation(summary = "Bean Validation：失败时由 ControllerResponseAdvice 转为 400 + 字段级错误")
    public Map<String, Object> validate(@Valid @RequestBody ValidateForm form) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", form.getName());
        result.put("age", form.getAge());
        return result;
    }

    @GetMapping("slow")
    @Operation(summary = "慢请求：耗时超过 app.slow.request.inSecond 时 SlowRequestAspect 打印 warn 日志")
    public Map<String, Object> slow(@RequestParam(defaultValue = "3000") long millis) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(millis);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sleptMillis", millis);
        result.put("costMillis", System.currentTimeMillis() - start);
        result.put("thresholdInSecond", "app.slow.request.inSecond");
        return result;
    }

    @PostMapping("repeat")
    @RepeatRequest(expiredTimeInMills = 3000)
    @Operation(summary = "3 秒内的重复提交会被拦截")
    public String repeat(@RequestParam(defaultValue = "order-001") String orderNo) {
        return "accepted:" + orderNo;
    }

    public static class ValidateForm {

        @NotBlank(message = "{demo.valid.name}")
        private String name;

        @Min(value = 1, message = "{demo.valid.age}")
        @Max(value = 120, message = "{demo.valid.age}")
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
