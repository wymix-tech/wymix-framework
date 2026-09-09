package com.travelsky.base.cache.configuration;

import com.travelsky.base.cache.manager.CompositeCacheManager;
import com.travelsky.base.cache.manager.CustomHazelcastCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配集成测试（ApplicationContextRunner）：
 * 验证同时引入 hazelcast-spring + caffeine 时可正常启动（验收 1）、
 * spring.application.name 缺失时可启动（验收 2）、
 * 业务自定义 CacheManager/KeyGenerator 有可行路径（验收 6）。
 */
class CacheAutoConfigurationIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class));

    @AfterEach
    void cleanUp() {
        // 关闭由测试上下文创建的 Hazelcast 实例
        com.hazelcast.core.Hazelcast.shutdownAll();
    }

    @Test
    void bothHazelcastAndCaffeineShouldStartWithCompositeCacheManager() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("cacheManager");
            assertThat(context.getBean("cacheManager", CacheManager.class))
                    .isInstanceOf(CompositeCacheManager.class);
            CompositeCacheManager composite = context.getBean("cacheManager", CompositeCacheManager.class);
            assertThat(composite.getPrimaryCacheManager())
                    .isInstanceOf(CustomHazelcastCacheManager.class);
            assertThat(composite.getLocalCacheManager())
                    .isInstanceOf(CaffeineCacheManager.class);
        });
    }

    @Test
    void missingSpringApplicationNameShouldFallbackToDefault() {
        // 不配置 spring.application.name，Hazelcast 分支应能启动且前缀回退 default
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            CacheManager cacheManager = context.getBean("cacheManager", CacheManager.class);
            assertThat(cacheManager.getCache("any")).isNotNull();
        });
    }

    @Test
    void businessCustomCacheManagerShouldNotConflict() {
        // 验收 6: 业务自定义 CacheManager 的推荐替换路径 = 关闭模块自动装配（cache.enabled=false）
        // 或 @Primary + 自定义 Bean 名；不再触发 CachingConfigurer 多实现异常
        new ApplicationContextRunner()
                .withPropertyValues("cache.enabled=false")
                .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class))
                .withUserConfiguration(BusinessCustomCacheConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("businessCacheManager", CacheManager.class))
                            .isInstanceOf(BusinessCustomCacheConfig.StaticCacheManager.class);
                });
    }

    @Test
    void businessCustomKeyGeneratorShouldOverrideDefault() {
        runner.withUserConfiguration(BusinessCustomKeyGeneratorConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("keyGenerator", KeyGenerator.class))
                    .isInstanceOf(BusinessCustomKeyGeneratorConfig.CustomKeyGenerator.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessCustomCacheConfig {

        @Bean
        @org.springframework.context.annotation.Primary
        public CacheManager businessCacheManager() {
            return new StaticCacheManager();
        }

        static class StaticCacheManager implements CacheManager {
            @Override
            public org.springframework.cache.Cache getCache(String name) {
                return null;
            }

            @Override
            public java.util.Collection<String> getCacheNames() {
                return java.util.Collections.emptyList();
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessCustomKeyGeneratorConfig {

        @Bean
        public KeyGenerator keyGenerator() {
            return new CustomKeyGenerator();
        }

        static class CustomKeyGenerator implements KeyGenerator {
            @Override
            public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
                return "custom";
            }
        }
    }
}
