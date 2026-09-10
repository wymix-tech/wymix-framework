package com.travelsky.base.cache.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配集成测试（ApplicationContextRunner）：模块已降级为纯 Caffeine 本地缓存。
 *
 * <p>覆盖：装配条件、规格绑定与缓存名归一化、默认 KeyGenerator 经切面生效、
 * 业务侧覆盖 KeyGenerator / CacheManager、CachingConfigurer 二选一语义。
 */
class CacheAutoConfigurationIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class));

    @Test
    void shouldRegisterCaffeineCacheManagerWithConfiguredSpecs() {
        runner.withPropertyValues(
                "caffeine.cache.sample-cache.maximumSize=100",
                "caffeine.cache.sample-cache.expireInSec=30").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(
                    CacheManagerConfiguration.SpecCacheManager.class);
            // 未声明的缓存名仍可懒建（默认规格）
            assertThat(context.getBean(CacheManager.class).getCache("anything")).isNotNull();
        });
    }

    @Test
    void normalizedNameShouldHitConfiguredSpec() {
        // @Cacheable(cacheNames = "SampleCache") 必须命中 caffeine.cache.sample-cache.*
        runner.withPropertyValues(
                "caffeine.cache.sample-cache.maximumSize=100",
                "caffeine.cache.sample-cache.expireInSec=30").run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            // 归一化后请求名解析到配置名：若不做归一化，这里会返回 null（配置名只注册了
            // sample-cache，Spring 不会把 SampleCache 映射过去）
            Cache cache = cacheManager.getCache("SampleCache");
            assertThat(cache).isNotNull();
            assertThat(cache).isInstanceOf(CaffeineCache.class);
            // 请求名不能派生出新的缓存名（配置名与请求名收敛到同一实例）
            assertThat(cache.getName()).isEqualTo("sample-cache");
            // 别名请求与全小写请求拿到同一实例
            assertThat(cacheManager.getCache("sample-cache")).isSameAs(cache);
            assertThat(cacheManager.getCacheNames()).containsExactly("sample-cache");
        });
    }

    @Test
    void getCacheNamesShouldOnlyExposeConfiguredAndRequestedNames() {
        runner.withPropertyValues("caffeine.cache.sample-cache.maximumSize=100").run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            assertThat(cacheManager.getCacheNames()).containsExactly("sample-cache");

            cacheManager.getCache("SampleCache");
            cacheManager.getCache("extra-cache");
            // 别名请求不产生新名字；未配置的请求名按请求名懒建并对外可见
            Collection<String> names = cacheManager.getCacheNames();
            assertThat(names).containsExactlyInAnyOrder("sample-cache", "extra-cache");
        });
    }

    @Test
    void undeclaredNamesShouldStayLazyBuildableAndSpecCachesKeepTheirSpec() {
        // 声明了规格的缓存名进入 CaffeineCacheManager 的静态清单；未声明的名字首次请求时
        // 应切回动态模式懒建，且规格缓存不能被默认规格覆盖（其缓存实例需被保留）
        runner.withPropertyValues(
                "caffeine.cache.spec-cache.maximumSize=100",
                "caffeine.cache.spec-cache.expireInSec=30").run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            Cache specCache = cacheManager.getCache("spec-cache");
            assertThat(specCache).isNotNull();
            specCache.put("survivor", "v");
            assertThat(cacheManager.getCacheNames()).containsExactly("spec-cache");

            // 触发动态模式切换
            Cache lazy = cacheManager.getCache("lazy-cache");
            assertThat(lazy).isNotNull();
            // 规格缓存内容不能因模式切换丢失（底层 CaffeineCache 适配器会被重建，
            // 但原生 Caffeine Cache 实例复用，数据保留）
            assertThat(cacheManager.getCache("spec-cache").get("survivor").get()).isEqualTo("v");
            // 已声明的清单不能因切换而丢失，未声明的名字按首次请求名可见
            assertThat(cacheManager.getCacheNames())
                    .containsExactlyInAnyOrder("spec-cache", "lazy-cache");

            // 规格仍然生效：maximumSize=100 的缓存写入 300 条并触发维护后，容量回落到 100
            // （Caffeine 的淘汰是维护驱动的，需显式 cleanUp 或再次访问后才收敛）
            for (int i = 0; i < 300; i++) {
                specCache.put("k" + i, "v" + i);
            }
            com.github.benmanes.caffeine.cache.Cache<?, ?> specNative =
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>) specCache.getNativeCache();
            specNative.cleanUp();
            assertThat(specNative.estimatedSize()).isLessThanOrEqualTo(100);
            // 懒建缓存使用默认规格（maximumSize=-1，不限容量），300 条全部保留
            for (int i = 0; i < 300; i++) {
                lazy.put("k" + i, "v" + i);
            }
            assertThat(((com.github.benmanes.caffeine.cache.Cache<?, ?>) lazy.getNativeCache()).estimatedSize())
                    .isEqualTo(300);
        });
    }

    @Test
    void moduleCanBeDisabledByProperty() {
        new ApplicationContextRunner()
                .withPropertyValues("cache.enabled=false")
                .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheManager.class);
                    assertThat(context).doesNotHaveBean(KeyGenerator.class);
                });
    }

    @Test
    void defaultKeyGeneratorShouldBeWiredThroughCachingConfigurer() {
        // Spring Cache 切面只从 CachingConfigurer 取 keyGenerator，不按名查找 Bean；
        // 本用例走切面调用（@Cacheable 未指定 keyGenerator），装配失效则回退
        // SimpleKeyGenerator，格式 key 查不到即失败
        runner.withUserConfiguration(AspectProbeConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            AspectProbeConfig.ProbeService probe = context.getBean(AspectProbeConfig.ProbeService.class);
            int before = AspectProbeConfig.ProbeService.invocations.get();
            probe.query("abc", 1);

            Cache cache = context.getBean(CacheManager.class).getCache("probe-cache");
            assertThat(cache.get("DEFAULT:CACHE:ProbeService:query:\"abc\"1")).isNotNull();
            // 二次调用命中缓存，回源计数器不增长
            probe.query("abc", 1);
            assertThat(AspectProbeConfig.ProbeService.invocations.get() - before).isEqualTo(1);
        });
    }

    @Test
    void businessCustomKeyGeneratorShouldOverrideDefault() {
        runner.withUserConfiguration(BusinessCustomKeyGeneratorConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("keyGenerator", KeyGenerator.class))
                    .isInstanceOf(BusinessCustomKeyGeneratorConfig.CustomKeyGenerator.class);
            assertThat(context.getBean(CachingConfigurer.class, CachingConfigurer.class).keyGenerator())
                    .isInstanceOf(BusinessCustomKeyGeneratorConfig.CustomKeyGenerator.class);
        });
    }

    @Test
    void businessCustomCacheManagerShouldTakeOverEntirely() {
        // 业务声明名为 cacheManager 的 Bean（或任意 Bean 名 + @Primary 并由业务自定义
        // CachingConfigurer 接管解析）时，模块不再注册自己的 CacheManager
        runner.withUserConfiguration(BusinessCacheManagerConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(BusinessCacheManager.class);
            assertThat(context).doesNotHaveBean(CacheManagerConfiguration.SpecCacheManager.class);
        });
    }

    @Test
    void businessCustomCacheManagerWithOtherNameAndPrimaryShouldTakeOver() {
        // 业务 Bean 名不是 cacheManager 时，模块的 cacheManager 仍会注册。
        // 两者共存时由业务侧的自定义 CachingConfigurer 决定用哪个（见
        // businessCachingConfigurerShouldWorkWhenModuleDisabled），或直接关闭模块。
        // 本用例锁定：模块缓存与业务缓存共存不冲突，且 @Primary 使按类型注入拿到业务 Bean
        runner.withUserConfiguration(BusinessPrimaryCacheManagerConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(BusinessCacheManager.class);
            assertThat(context.getBean("cacheManager", CacheManager.class))
                    .isInstanceOf(CacheManagerConfiguration.SpecCacheManager.class);
        });
    }

    @Test
    void businessCachingConfigurerShouldConflictUnlessModuleDisabled() {
        // 模块内嵌 CacheCachingConfigurer 为无条件注册，与业务侧实现是「二选一」：
        // 不关闭模块即 2 个实现，启动直接失败（Spring 启动期收集校验）
        runner.withUserConfiguration(BusinessCachingConfigurerConfig.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("2 implementations of CachingConfigurer");
        });
    }

    @Test
    void businessCachingConfigurerShouldWorkWhenModuleDisabled() {
        new ApplicationContextRunner()
                .withPropertyValues("cache.enabled=false")
                .withConfiguration(AutoConfigurations.of(CacheManagerConfiguration.class))
                .withUserConfiguration(BusinessCachingConfigurerConfig.class, AspectProbeConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CachingConfigurer.class))
                            .isInstanceOf(BusinessCachingConfigurerConfig.BusinessCachingConfigurer.class);
                    AspectProbeConfig.ProbeService probe = context.getBean(AspectProbeConfig.ProbeService.class);
                    int before = AspectProbeConfig.ProbeService.invocations.get();
                    probe.query("abc", 1);
                    // 业务 keyGenerator 返回固定 key，二次调用命中缓存
                    probe.query("abc", 1);
                    assertThat(AspectProbeConfig.ProbeService.invocations.get() - before).isEqualTo(1);
                });
    }

    private static Object unwrap(Object value) {
        // allowNullValues 默认 true，Spring 用 ValueWrapper 包裹返回值
        return (value instanceof Cache.ValueWrapper) ? ((Cache.ValueWrapper) value).get() : value;
    }

    @Test
    void caffeineCacheShouldSupportRetrieveInDefaultAsyncCacheMode() {
        // 默认异步缓存模式下，Spring 的 CaffeineCache 才能真正实现 retrieve
        // （普通 Cache 形态会抛 IllegalStateException: No Caffeine AsyncCache available）
        runner.withPropertyValues("caffeine.cache.sample-cache.maximumSize=10").run(context -> {
            Cache cache = context.getBean(CacheManager.class).getCache("sample-cache");
            assertThat(cache).isNotNull();
            // allowNullValues 默认 true，Spring 用 ValueWrapper 包裹返回值
            assertThat(unwrap(cache.retrieve("k",
                    () -> java.util.concurrent.CompletableFuture.completedFuture("v")).join()))
                    .isEqualTo("v");
            assertThat(unwrap(cache.retrieve("k").join())).isEqualTo("v");
            // 未声明的缓存名同样具备 retrieve 能力（默认规格 + 懒建路径）
            Cache dynamic = context.getBean(CacheManager.class).getCache("not-declared");
            assertThat(unwrap(dynamic.retrieve("k",
                    () -> java.util.concurrent.CompletableFuture.completedFuture("v2")).join()))
                    .isEqualTo("v2");
        });
    }

    @Test
    void asyncCacheModeCanBeDisabled() {
        // 关闭异步缓存模式：retrieve 不再可用（能力显式对齐），普通读写不受影响
        runner.withPropertyValues("caffeine.asyncCacheMode=false").run(context -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            Cache cache = cacheManager.getCache("plain");
            cache.put("k", "v");
            assertThat(cache.get("k").get()).isEqualTo("v");
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
            static final AtomicInteger invocations = new AtomicInteger();

            // 不指定 keyGenerator：key 只能经 CachingConfigurer 进入切面
            @Cacheable(cacheNames = "probe-cache")
            public String query(String keyword, Integer page) {
                invocations.incrementAndGet();
                return "value:" + keyword + ":" + page;
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

    @Configuration(proxyBeanMethods = false)
    static class BusinessCacheManagerConfig {

        @Bean
        public CacheManager cacheManager() {
            return new BusinessCacheManager();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessPrimaryCacheManagerConfig {

        @Bean
        @org.springframework.context.annotation.Primary
        public CacheManager businessCacheManager() {
            return new BusinessCacheManager();
        }
    }

    static class BusinessCacheManager implements CacheManager {
        @Override
        public Cache getCache(String name) {
            return null;
        }

        @Override
        public Collection<String> getCacheNames() {
            return java.util.Collections.emptyList();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessCachingConfigurerConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        public CachingConfigurer businessCachingConfigurer() {
            return new BusinessCachingConfigurer();
        }

        static class BusinessCachingConfigurer implements CachingConfigurer {
            @Override
            public KeyGenerator keyGenerator() {
                return (target, method, params) -> "business-key";
            }
        }
    }
}
