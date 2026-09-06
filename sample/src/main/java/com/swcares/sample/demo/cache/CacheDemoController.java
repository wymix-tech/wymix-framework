package com.swcares.sample.demo.cache;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * travelsky_dev_cache 示例入口。
 */
@RestController
@RequestMapping("demo/cache")
@Tag(name = "cache 模块示例")
public class CacheDemoController {

    private final CacheDemoService cacheDemoService;

    public CacheDemoController(CacheDemoService cacheDemoService) {
        this.cacheDemoService = cacheDemoService;
    }

    @GetMapping("{key}")
    @Operation(summary = "读取缓存（未命中则回源）")
    public String get(@PathVariable String key) {
        return cacheDemoService.load(key);
    }

    @PutMapping("{key}")
    @Operation(summary = "刷新缓存")
    public String refresh(@PathVariable String key) {
        return cacheDemoService.refresh(key);
    }

    @DeleteMapping("{key}")
    @Operation(summary = "清除缓存")
    public String evict(@PathVariable String key) {
        cacheDemoService.evict(key);
        return "evicted:" + key;
    }
}
