package com.travelsky.base.cache.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheManagerConfiguration 单元测试：
 * 验证 keyGenerator 行为不变（既有断言），以及 Caffeine 配置校验逻辑。
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
    void shouldNotImplementCachingConfigurer() {
        // P2-6: 装配类不再实现 CachingConfigurer，避免业务侧自定义扩展点时多实现冲突
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

    @org.junit.jupiter.api.Nested
    class CaffeineConfigValidationTest {

        private CacheManagerConfiguration.CaffeineCacheConfiguration caffeineConfiguration;

        @BeforeEach
        void setUp() {
            caffeineConfiguration = configuration.new CaffeineCacheConfiguration();
        }

        private com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig config(
                long maximumSize, long maximumWeight, long refreshAfterWriterInSec) {
            com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig config =
                    new com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig();
            config.setMaximumSize(maximumSize);
            config.setMaximumWeight(maximumWeight);
            config.setRefreshAfterWriterInSec(refreshAfterWriterInSec);
            return config;
        }

        @Test
        void maximumWeightAloneShouldWorkWithWeigher() {
            // P1-4: maximumWeight 可单独使用（需提供 weigher）
            com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig config =
                    config(-1, 100, -1);
            config.setWeigher(TestWeigher.class.getName());
            assertDoesNotThrow(() -> caffeineConfiguration.buildeCaffeine(config));
        }

        @Test
        void maximumSizeAndMaximumWeightTogetherShouldFailWithReadableError() {
            // P1-4: 两者同时配置时报可读错误而非 Caffeine 内部异常
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> caffeineConfiguration.buildeCaffeine(config(999, 100, -1)));
            assertTrue(ex.getMessage().contains("互斥"));
        }

        @Test
        void maximumWeightWithoutWeigherShouldFailWithReadableError() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> caffeineConfiguration.buildeCaffeine(config(-1, 100, -1)));
            assertTrue(ex.getMessage().contains("weigher"));
        }

        @Test
        void defaultMaximumSizeShouldNotTriggerLimit() {
            // P1-4: 默认 maximumSize=-1，不再默认调用 maximumSize(999)
            com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig config =
                    new com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig();
            assertEquals(-1, config.getMaximumSize());
            assertDoesNotThrow(() -> caffeineConfiguration.buildeCaffeine(config));
        }

        @Test
        void refreshAfterWritePositiveShouldFailWithReadableError() {
            // P1-3: 未提供 CacheLoader，refreshAfterWriterInSec > 0 直接报可读错误
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> caffeineConfiguration.buildeCaffeine(config(-1, -1, 60)));
            assertTrue(ex.getMessage().contains("refreshAfterWriterInSec"));
        }

        @Test
        void weigherWithWrongTypeShouldFailWithReadableError() {
            com.travelsky.base.cache.properties.CaffeineCacheProperties.CaffeineConfig config =
                    config(-1, 100, -1);
            config.setWeigher(String.class.getName());
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> caffeineConfiguration.buildeCaffeine(config));
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

    static class SampleService {
        @SuppressWarnings("unused")
        public void query(String keyword, Integer page) {
        }
    }
}
