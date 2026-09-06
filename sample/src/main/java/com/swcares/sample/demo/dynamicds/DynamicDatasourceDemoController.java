package com.swcares.sample.demo.dynamicds;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * travelsky_dev_dynamic_datasource 示例入口。
 */
@RestController
@RequestMapping("demo/dynamicDatasource")
@Tag(name = "dynamic_datasource 模块示例")
public class DynamicDatasourceDemoController {

    private final DynamicDatasourceDemoService dynamicDatasourceDemoService;

    public DynamicDatasourceDemoController(DynamicDatasourceDemoService dynamicDatasourceDemoService) {
        this.dynamicDatasourceDemoService = dynamicDatasourceDemoService;
    }

    @GetMapping("count")
    @Operation(summary = "分别统计主库与 slave 库的用户数")
    public Map<String, Object> count() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primary", dynamicDatasourceDemoService.countOnPrimary());
        result.put("slave", dynamicDatasourceDemoService.countOnSlave());
        result.put("slaveRegistered", dynamicDatasourceDemoService.exists("slave"));
        return result;
    }

    @GetMapping("count/{name}")
    @Operation(summary = "编程式切换数据源后统计")
    public long countByName(@PathVariable String name) {
        return dynamicDatasourceDemoService.countByContextHolder(name);
    }
}
