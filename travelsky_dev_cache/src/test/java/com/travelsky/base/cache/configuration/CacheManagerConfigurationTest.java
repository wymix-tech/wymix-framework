package com.travelsky.base.cache.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheManagerConfiguration 单元测试：
 * 验证废弃的 CachingConfigurerSupport 替换为 CachingConfigurer 后 keyGenerator 行为不变。
 */
class CacheManagerConfigurationTest {

    private CacheManagerConfiguration configuration;

    @BeforeEach
    void setUpAppName() throws Exception {
        configuration = new CacheManagerConfiguration();
        // appName 由 @Value 注入，单测中反射注入模拟值
        java.lang.reflect.Field field = CacheManagerConfiguration.class.getDeclaredField("appName");
        field.setAccessible(true);
        field.set(configuration, "default");
    }

    @Test
    void shouldImplementCachingConfigurer() {
        assertInstanceOf(org.springframework.cache.annotation.CachingConfigurer.class, configuration);
    }

    @Test
    void keyGeneratorShouldBuildPrefixedKey() throws Exception {
        KeyGenerator keyGenerator = configuration.keyGenerator();
        assertNotNull(keyGenerator);

        Method method = SampleService.class.getMethod("query", String.class, Integer.class);
        Object key = keyGenerator.generate(new SampleService(), method, "abc", 1);
        // String 参数经 JsonUtils.entityToJson 序列化，带引号
        assertEquals("DEFAULT:CACHE:SampleService:query:\"abc\"1", key);
    }

    static class SampleService {
        @SuppressWarnings("unused")
        public void query(String keyword, Integer page) {
        }
    }
}
