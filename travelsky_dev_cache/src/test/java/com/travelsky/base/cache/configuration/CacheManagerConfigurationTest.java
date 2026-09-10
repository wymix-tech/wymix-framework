package com.travelsky.base.cache.configuration;

import com.travelsky.base.cache.properties.CaffeineCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 装配类单元测试：keyGenerator 行为（既有断言，不得回归）与 Caffeine 规格校验。
 */
class CacheManagerConfigurationTest {

    private CacheManagerConfiguration configuration;

    @BeforeEach
    void setUpAppName() throws Exception {
        configuration = new CacheManagerConfiguration();
        // appName 由 @Value 注入，单测中反射注入模拟值
        Field field = CacheManagerConfiguration.class.getDeclaredField("appName");
        field.setAccessible(true);
        field.set(configuration, "default");
    }

    @Test
    void configurationShouldNotBeCachingConfigurerItself() {
        // CachingConfigurer 由内嵌 CacheCachingConfigurer 承担，装配类本身不再实现，
        // 避免业务侧扩展时出现意外的多实现来源
        assertFalse(configuration instanceof org.springframework.cache.annotation.CachingConfigurer);
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

    @Test
    void keyGeneratorShouldRenderNullParamAsNullLiteral() throws Exception {
        KeyGenerator keyGenerator = configuration.keyGenerator();
        Method method = SampleService.class.getMethod("query", String.class, Integer.class);
        Object key = keyGenerator.generate(new SampleService(), method, null, 1);
        assertEquals("DEFAULT:CACHE:SampleService:query:null1", key);
    }

    @Nested
    class CaffeineConfigValidationTest {

        private CaffeineCacheProperties.CaffeineConfig config(
                long maximumSize, long maximumWeight, long refreshAfterWriterInSec) {
            CaffeineCacheProperties.CaffeineConfig config = new CaffeineCacheProperties.CaffeineConfig();
            config.setMaximumSize(maximumSize);
            config.setMaximumWeight(maximumWeight);
            config.setRefreshAfterWriterInSec(refreshAfterWriterInSec);
            return config;
        }

        @Test
        void maximumWeightAloneShouldWorkWithWeigher() {
            CaffeineCacheProperties.CaffeineConfig config = config(-1, 100, -1);
            config.setWeigher(TestWeigher.class.getName());
            assertDoesNotThrow(() -> CacheManagerConfiguration.buildCaffeine(config));
        }

        @Test
        void maximumSizeAndMaximumWeightTogetherShouldFailWithReadableError() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> CacheManagerConfiguration.buildCaffeine(config(999, 100, -1)));
            assertTrue(ex.getMessage().contains("互斥"));
        }

        @Test
        void maximumWeightWithoutWeigherShouldFailWithReadableError() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> CacheManagerConfiguration.buildCaffeine(config(-1, 100, -1)));
            assertTrue(ex.getMessage().contains("weigher"));
        }

        @Test
        void defaultMaximumSizeShouldNotTriggerLimit() {
            CaffeineCacheProperties.CaffeineConfig config = new CaffeineCacheProperties.CaffeineConfig();
            assertEquals(-1, config.getMaximumSize());
            assertDoesNotThrow(() -> CacheManagerConfiguration.buildCaffeine(config));
        }

        @Test
        void refreshAfterWritePositiveShouldFailWithReadableError() {
            // 未提供 CacheLoader，refreshAfterWriterInSec > 0 直接报可读错误
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> CacheManagerConfiguration.buildCaffeine(config(-1, -1, 60)));
            assertTrue(ex.getMessage().contains("refreshAfterWriterInSec"));
        }

        @Test
        void weigherOutsideAllowedPackagesShouldFail() {
            // weigher 类名来自配置，限制包前缀白名单，避免加载 classpath 上任意类
            CaffeineCacheProperties.CaffeineConfig config = config(-1, 100, -1);
            config.setWeigher("java.lang.String");
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> CacheManagerConfiguration.buildCaffeine(config));
            assertTrue(ex.getMessage().contains("包范围"));
        }

        @Test
        void weigherWithWrongTypeShouldFailWithReadableError() {
            CaffeineCacheProperties.CaffeineConfig config = config(-1, 100, -1);
            // 白名单包内但未实现 Weigher 的类，验证类型校验仍然生效
            config.setWeigher(SampleHolder.class.getName());
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> CacheManagerConfiguration.buildCaffeine(config));
            assertTrue(ex.getMessage().contains("Weigher"));
        }
    }

    /**
     * 测试用权重函数：每个条目权重恒为 1。
     */
    static class TestWeigher implements com.github.benmanes.caffeine.cache.Weigher<Object, Object> {
        @Override
        public int weigh(Object key, Object value) {
            return 1;
        }
    }

    /**
     * 白名单包内但未实现 Weigher 的类，用于类型校验用例。
     */
    static class SampleHolder {
    }

    static class SampleService {
        @SuppressWarnings("unused")
        public void query(String keyword, Integer page) {
        }
    }
}
