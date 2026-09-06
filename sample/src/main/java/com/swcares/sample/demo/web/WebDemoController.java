package com.swcares.sample.demo.web;

import com.travelsky.base.web.aop.RepeatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * travelsky_dev_web 示例：统一响应包装与防重提交注解。
 *
 * <p>web 模块的 {@code ControllerResponseAdvice} 会把 Controller 返回值统一包装为
 * {@code ResultVO}，因此这里直接返回业务对象即可。
 * {@link RepeatRequest} 用于抑制短时间内的重复请求（expiredTimeInMills 为去重窗口）。
 */
@RestController
@RequestMapping("demo/web")
@Tag(name = "web 模块示例")
public class WebDemoController {

    @GetMapping("payload")
    @Operation(summary = "返回业务对象，由 ControllerResponseAdvice 自动包装为 ResultVO")
    public Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("module", "travelsky_dev_web");
        payload.put("advice", "ControllerResponseAdvice");
        return payload;
    }

    @PostMapping("repeat")
    @RepeatRequest(expiredTimeInMills = 3000)
    @Operation(summary = "3 秒内的重复提交会被拦截")
    public String repeat(@RequestParam(defaultValue = "order-001") String orderNo) {
        return "accepted:" + orderNo;
    }
}
