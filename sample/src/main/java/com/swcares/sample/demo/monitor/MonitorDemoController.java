package com.swcares.sample.demo.monitor;

import com.travelsky.base.core.context.ApplicationContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * travelsky_dev_monitor 示例。
 *
 * <p>monitor 模块提供两件事：
 * <ol>
 *     <li>{@code MonitorConfiguration} 注册 {@code IgnoreUrlContext} 放行 {@code /actuator/**}
 *     （开关 {@code travelsky.monitor.enabled}）；</li>
 *     <li>{@code MonitorApplicationStartedEventListener} 在环境准备阶段注入 Actuator/Prometheus
 *     默认值，由 {@code META-INF/spring.factories} 自动注册，原生启动方式即可生效。</li>
 * </ol>
 * 本控制器不重复实现监控能力，只把「当前开放了哪些 actuator 端点」暴露出来便于核对。
 */
@RestController
@RequestMapping("demo/monitor")
@Tag(name = "monitor 模块示例")
public class MonitorDemoController {

    private final Environment environment;

    @Value("${spring.application.name:sample}")
    private String appName;

    public MonitorDemoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("endpoints")
    @Operation(summary = "列出当前开放的 actuator 端点")
    public Map<String, Object> endpoints() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appName", appName);
        result.put("exposureInclude", environment.getProperty("management.endpoints.web.exposure.include", ""));
        result.put("monitorEnabled", environment.getProperty("travelsky.monitor.enabled", "true"));
        result.put("paths", resolveEndpointPaths());
        return result;
    }

    private Map<String, String> resolveEndpointPaths() {
        Map<String, String> paths = new TreeMap<>();
        WebMvcEndpointHandlerMapping mapping = ApplicationContextHolder
                .getApplicationContext()
                .getBeanProvider(WebMvcEndpointHandlerMapping.class)
                .getIfAvailable();
        if (mapping == null) {
            return paths;
        }
        mapping.getEndpoints().forEach(endpoint ->
                paths.put(endpoint.getEndpointId().toString(), endpoint.getRootPath()));
        return paths;
    }
}
