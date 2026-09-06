package com.swcares.sample.demo.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * travelsky_dev_cache 示例：标准的 Spring Cache 注解用法。
 *
 * <p>cache 模块已通过 {@code @EnableCaching} 开启缓存并注册了自定义 KeyGenerator
 * （key = 应用名:CACHE:类名:方法名:参数），具体 CacheManager 由 classpath 上的实现决定，
 * sample 选用 Caffeine，缓存规格见 {@code application-cache.yml}。
 */
@Service
public class CacheDemoService {

    /**
     * 首次访问会执行方法体并写入缓存，之后直接命中缓存返回。
     */
    @Cacheable(cacheNames = "sampleCache", key = "#key")
    public String load(String key) {
        return key + "@" + LocalDateTime.now();
    }

    /**
     * 每次都执行方法体并刷新缓存。
     */
    @CachePut(cacheNames = "sampleCache", key = "#key")
    public String refresh(String key) {
        return key + "@" + LocalDateTime.now();
    }

    /**
     * 删除指定 key 的缓存。
     */
    @CacheEvict(cacheNames = "sampleCache", key = "#key")
    public void evict(String key) {
        // 仅需注解，方法体可为空
    }
}
