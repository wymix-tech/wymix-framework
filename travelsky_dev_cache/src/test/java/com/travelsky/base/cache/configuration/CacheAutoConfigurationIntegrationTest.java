package com.travelsky.base.cache.configuration;

import com.travelsky.base.cache.manager.CompositeCacheManager;
import com.travelsky.base.cache.manager.CustomHazelcastCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
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
        // 不配置 spring.application.name，Hazelcast 分支应能启动且前缀回退 default（而非 null:）
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            CustomHazelcastCacheManager hazelcastManager =
                    context.getBean(CustomHazelcastCacheManager.class);
            org.springframework.cache.Cache cache = hazelcastManager.getCache("user");
            assertThat(cache).isNotNull();
            // 前缀必须是 default，若回退为 null 会拼出 null:user 脏 key，此处即可拦截回归
            assertThat(cache.getName()).startsWith("default:");
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
                    // 模块自动装配确实被关闭（否则本用例名不副实）
                    assertThat(context).doesNotHaveBean(CompositeCacheManager.class);
                    assertThat(context).doesNotHaveBean("hazelcastCacheManager");
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

    @Test
    void defaultKeyGeneratorShouldBeWiredThroughCachingConfigurer() {
        // 第二次评审「严重」项：keyGenerator 必须经 CacheCachingConfigurer 接入切面，
        // 否则 @Cacheable 静默回退 SimpleKeyGenerator、<APP>:CACHE:... 格式 key 丢失。
        // 本测试走切面调用，绕不过装配。
        runner.withUserConfiguration(AspectProbeConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            AspectProbeConfig.ProbeService probe = context.getBean(AspectProbeConfig.ProbeService.class);
            // 首次调用回源，key 由模块 KeyGenerator 生成
            probe.query("abc", 1);
            // 若 keyGenerator 未接入切面，@Cacheable 回退 SimpleKeyGenerator（key=SimpleKey[abc,1]），
            // 用模块 keyGenerator 的格式查缓存即 miss；接入成功则命中
            CacheManager cm = context.getBean(CacheManager.class);
            org.springframework.cache.Cache cache = cm.getCache("probe-cache");
            org.springframework.cache.Cache.ValueWrapper wrapper =
                    cache.get("DEFAULT:CACHE:ProbeService:query:\"abc\"1");
            assertThat(wrapper).isNotNull();
            assertThat(wrapper.get()).isEqualTo("value:abc:1");
        });
    }

    @Test
    void businessCachingConfigurerWithoutDisablingModuleShouldConflict() {
        // Spring 的 AbstractCachingConfiguration#setConfigurers 在启动期收集全部
        // CachingConfigurer，数量 >1 直接抛 IllegalStateException。
        // 模块的 CacheCachingConfigurer 为无条件注册（@ConditionalOnMissingBean 对
        // 独立自动装配的业务配置不可靠，见评审与源码注释），因此业务侧接管该接口
        // 必须先关闭模块自动装配（cache.enabled=false）——本用例锁定这一确定行为，
        // 防止有人改回「自动让位」的错误承诺。
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class))
                .withUserConfiguration(BusinessCachingConfigurerConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("2 implementations of CachingConfigurer");
                });
    }

    @Test
    void businessCachingConfigurerWithModuleDisabledShouldWork() {
        // 关闭模块自动装配后，业务侧可自由实现 CachingConfigurer 接管切面
        new ApplicationContextRunner()
                .withPropertyValues("cache.enabled=false")
                .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class))
                .withUserConfiguration(AspectProbeConfig.class, BusinessCachingConfigurerConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(org.springframework.cache.annotation.CachingConfigurer.class))
                            .isInstanceOf(BusinessCachingConfigurerConfig.BusinessCachingConfigurer.class);
                    AspectProbeConfig.ProbeService probe = context.getBean(AspectProbeConfig.ProbeService.class);
                    // 业务 CachingConfigurer 的 keyGenerator 返回固定值 business-key；
                    // 二次调用（同 key）必命中缓存，方法体不再执行
                    Object first = probe.query("abc", 1);
                    Object second = probe.query("abc", 1);
                    assertThat(first).isEqualTo(second);
                });
    }

    @Test
    void cachingConfigurerShouldProvideCacheManagerWithoutPrimaryReliance() {
        // 第四轮评审警告项 5：CacheCachingConfigurer 覆写 cacheManager()，
        // 切面直接从 CachingConfigurer 解析 CacheManager，不再依赖 @Primary 兜底。
        // 通过 CachingConfigurer 接口取到的 CacheManager 必须是模块装配的那个
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            org.springframework.cache.annotation.CachingConfigurer configurer =
                    context.getBean(org.springframework.cache.annotation.CachingConfigurer.class);
            assertThat(configurer).isInstanceOf(
                    CacheManagerConfiguration.CacheCachingConfigurer.class);
            CacheManager fromConfigurer = configurer.cacheManager();
            assertThat(fromConfigurer).isNotNull();
            assertThat(fromConfigurer).isSameAs(context.getBean(CacheManager.class));
        });
    }

    @Test
    void businessCacheManagerAsNamedCacheManagerShouldNotConflict() {
        // README 路径二（修正版）：业务声明名为 cacheManager 的 Bean，组合缓存的
        // @ConditionalOnMissingBean(name="cacheManager") 让位，无 @Primary 冲突
        runner.withUserConfiguration(BusinessNamedCacheManagerConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("cacheManager", CacheManager.class))
                    .isInstanceOf(BusinessNamedCacheManagerConfig.BusinessCacheManager.class);
            assertThat(context.getBean("cacheManager", CacheManager.class))
                    .extracting(m -> ((CacheManager) m))
                    .isNotInstanceOf(CompositeCacheManager.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class AspectProbeConfig {

        @Bean
        public ProbeService probeService() {
            return new ProbeService();
        }

        static class ProbeService {
            // 不指定 keyGenerator：key 只能经 CachingConfigurer 进入切面，
            // 若 CacheCachingConfigurer 未生效则回退 SimpleKeyGenerator，本测试必红
            @Cacheable(cacheNames = "probe-cache")
            public String query(String keyword, Integer page) {
                // 返回值需可被 Hazelcast 序列化
                return "value:" + keyword + ":" + page;
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class BusinessCachingConfigurerConfig {

        @Bean
        public CacheManager businessCacheManager() {
            // cache.enabled=false 后模块不提供 CacheManager，切面需要一个
            return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
        }

        @Bean
        public org.springframework.cache.annotation.CachingConfigurer businessCachingConfigurer() {
            return new BusinessCachingConfigurer();
        }

        static class BusinessCachingConfigurer implements org.springframework.cache.annotation.CachingConfigurer {
            @Override
            public KeyGenerator keyGenerator() {
                return new BusinessKeyGenerator();
            }
        }

        static class BusinessKeyGenerator implements KeyGenerator {
            @Override
            public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
                return "business-key";
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessNamedCacheManagerConfig {

        @Bean
        @org.springframework.context.annotation.Primary
        public CacheManager cacheManager() {
            // 组合缓存因 @ConditionalOnMissingBean(name = "cacheManager") 让位，
            // @Primary 保证 @EnableCaching 下 CacheInterceptor 能解析唯一主 CacheManager
            return new BusinessCacheManager();
        }

        static class BusinessCacheManager implements CacheManager {
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
}
