package com.travelsky.base.cache.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.travelsky.base.cache.manager.CompositeCacheManager;
import com.travelsky.base.cache.manager.CustomHazelcastCacheManager;
import com.travelsky.base.cache.properties.CaffeineCacheProperties;
import com.travelsky.base.cache.properties.HazelcastCacheProperties;
import com.travelsky.base.utils.json.JsonUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
@AutoConfiguration
@ConditionalOnProperty(prefix = "cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class CacheManagerConfiguration {

    private static final String SPLITTER = ":";

    @Value("${spring.application.name:" + CustomHazelcastCacheManager.DEFAULT_APP_PREFIX + "}")
    private String appName;

    /**
     * 自定义 KeyGenerator（{@code <APP大写>:CACHE:<类名>:<方法名>:<参数JSON>}）。
     * <p>注意：Spring Cache 切面只从 {@link CachingConfigurer} 获取默认 keyGenerator，
     * <b>不会</b>按名查找容器中的 {@code keyGenerator} Bean。本 Bean 由下方内嵌的
     * {@link CacheCachingConfigurer} 接入切面；业务侧可通过声明同名
     * {@code keyGenerator} Bean 覆盖本默认实现。注意：模块固定注册
     * {@code CachingConfigurer}，业务侧如需自定义该接口须先整体关闭模块自动装配。
     */
    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() {
        return ((o, method, objects) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(appName.toUpperCase()).append(SPLITTER);
            sb.append("CACHE").append(SPLITTER);
            sb.append(o.getClass().getSimpleName()).append(SPLITTER);
            sb.append(method.getName()).append(SPLITTER);
            for (Object obj : objects) {
                sb.append(obj == null ? "null" : JsonUtils.entityToJson(obj));
            }
            return sb.toString();
        });
    }

    @Configuration
    @ConditionalOnClass(HazelcastInstance.class)
    @EnableConfigurationProperties(HazelcastCacheProperties.class)
    public class HazelcastCacheConfiguration {

        @Autowired
        private HazelcastCacheProperties hazelcastCacheProperties;

        /**
         * Hazelcast 实例交由 Spring 容器托管生命周期，应用关闭时自动 shutdown。
         */
        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(HazelcastInstance.class)
        public HazelcastInstance hazelcastInstance() {
            return Hazelcast.newHazelcastInstance(buildConfig());
        }

        private Config buildConfig() {
            Config config = new Config(hazelcastCacheProperties.getInstanceName());
            config.getNetworkConfig().getRestApiConfig().setEnabled(true);

            JoinConfig joinConfig = config.getNetworkConfig().getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);
            TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
            tcpIpConfig.setEnabled(hazelcastCacheProperties.isTcpIpEnabled());
            for (String member : hazelcastCacheProperties.getMembers()) {
                tcpIpConfig.addMember(member);
            }
            config.setProperty("hazelcast.local.public.address", hazelcastCacheProperties.getPublicAddress());

            Map<String, HazelcastCacheProperties.ExpiredConfig> namespace = hazelcastCacheProperties.getNamespace();
            if (namespace == null || !namespace.containsKey("default")) {
                MapConfig mapConfig = new MapConfig(appName + ":default");
                mapConfig.setTimeToLiveSeconds(86400);
                mapConfig.setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.LRU)
                        .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                        .setSize(EvictionConfig.DEFAULT_MAX_ENTRY_COUNT * 999)
                );
                config.addMapConfig(mapConfig);
            }
            if (namespace != null) {
                namespace.forEach((k, v) -> {
                    MapConfig mapConfig = new MapConfig(appName + ":" + k);
                    mapConfig.setTimeToLiveSeconds(v.getTimeToLiveInSeconds());
                    mapConfig.setEvictionConfig(new EvictionConfig()
                            .setEvictionPolicy(EvictionPolicy.getById(v.getEvictionPolicy()))
                            .setMaxSizePolicy(MaxSizePolicy.getById(v.getMaxSizePolicy()))
                            .setSize(v.getSize())
                    );
                    config.addMapConfig(mapConfig);
                });
            }

            return config;
        }

        @Bean
        public CacheManager hazelcastCacheManager(HazelcastInstance hazelcastInstance) {
            CustomHazelcastCacheManager cacheManager = new CustomHazelcastCacheManager(appName);
            cacheManager.setHazelcastInstance(hazelcastInstance);
            cacheManager.setHazelcastCacheProperties(hazelcastCacheProperties);
            return cacheManager;
        }
    }

    @Configuration
    @ConditionalOnClass(Caffeine.class)
    @EnableConfigurationProperties(CaffeineCacheProperties.class)
    public class CaffeineCacheConfiguration {

        @Autowired
        private CaffeineCacheProperties caffeineCacheProperties;

        @Bean
        public CacheManager caffeineCacheManager() {
            Map<String, CaffeineCacheProperties.CaffeineConfig> cache = caffeineCacheProperties.getCache();
            if (CollectionUtils.isEmpty(cache)) {
                CaffeineCacheProperties.CaffeineConfig caffeineConfig = new CaffeineCacheProperties.CaffeineConfig();
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();
                cacheManager.setCaffeine(buildeCaffeine(caffeineConfig));
                return cacheManager;
            }

            List<CaffeineCache> caches = new ArrayList<>();
            for (Map.Entry<String, CaffeineCacheProperties.CaffeineConfig> entry : cache.entrySet()) {
                caches.add(new CaffeineCache(entry.getKey(), buildeCaffeine(entry.getValue()).build()));
            }
            SimpleCacheManager simpleCacheManager = new SimpleCacheManager() {
                @Override
                protected Cache getMissingCache(String name) {
                    return new CaffeineCache(name, buildeCaffeine(new CaffeineCacheProperties.CaffeineConfig()).build());
                }
            };
            simpleCacheManager.setCaches(caches);
            return simpleCacheManager;
        }

        Caffeine<Object, Object> buildeCaffeine(CaffeineCacheProperties.CaffeineConfig config) {
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
            caffeine.initialCapacity(config.getInitialCapacity());
            if (config.getMaximumSize() > 0 && config.getMaximumWeight() > 0) {
                throw new IllegalStateException(
                        "caffeine.cache.<name>.maximumSize 与 maximumWeight 互斥，只能配置其中一个（"
                                + "maximumSize=" + config.getMaximumSize()
                                + ", maximumWeight=" + config.getMaximumWeight() + "）");
            }
            if (config.getMaximumSize() > 0) {
                caffeine.maximumSize(config.getMaximumSize());
            }
            if (config.getMaximumWeight() > 0) {
                if (config.getWeigher() == null || config.getWeigher().isBlank()) {
                    throw new IllegalStateException(
                            "配置 caffeine.cache.<name>.maximumWeight 时必须同时提供 weigher（"
                                    + "maximumWeight=" + config.getMaximumWeight() + "）");
                }
                caffeine.maximumWeight(config.getMaximumWeight())
                        .weigher(newWeightedWeigher(config.getWeigher()));
            }
            if (config.getPolicy() == CaffeineCacheProperties.Policy.AFTER_WRITE) {
                caffeine.expireAfterWrite(config.getExpireInSec(), TimeUnit.SECONDS);
            } else {
                caffeine.expireAfterAccess(config.getExpireInSec(), TimeUnit.SECONDS);
            }
            if (config.getRefreshAfterWriterInSec() > 0) {
                throw new IllegalStateException(
                        "caffeine.cache.<name>.refreshAfterWriterInSec 需要配合 CacheLoader 构建 LoadingCache，"
                                + "当前模块未提供回源实现，请勿配置该选项（"
                                + "refreshAfterWriterInSec=" + config.getRefreshAfterWriterInSec() + "）");
            }
            caffeine.evictionListener(new RemovalListener<Object, Object>() {
                @Override
                public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {

                }
            });
            caffeine.removalListener(new RemovalListener<Object, Object>() {
                @Override
                public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {

                }
            });
            return caffeine.recordStats();
        }

        /**
         * 按全限定类名实例化 Caffeine 权重函数，类型不匹配或实例化失败时抛出可读异常。
         */
        @SuppressWarnings("unchecked")
        private com.github.benmanes.caffeine.cache.Weigher<Object, Object> newWeightedWeigher(String weigherClass) {
            String trimmed = weigherClass.trim();
            // 类名来自配置文件，限制包前缀白名单，避免配置被污染时加载并实例化 classpath 上任意类
            if (!trimmed.startsWith("com.travelsky.") && !trimmed.startsWith("com.github.benmanes.")) {
                throw new IllegalStateException(
                        "weigher 不在允许的包范围内（仅支持 com.travelsky.* 或 com.github.benmanes.*）");
            }
            Object instance;
            try {
                Class<?> clazz = Class.forName(trimmed);
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("无法实例化 weigher（需提供 public 无参构造，"
                        + "且类名需在允许的包范围内）", ex);
            }
            if (!(instance instanceof com.github.benmanes.caffeine.cache.Weigher)) {
                throw new IllegalStateException("weigher 必须实现 com.github.benmanes.caffeine.cache.Weigher"
                        + "（实际类型: " + instance.getClass().getName() + "）");
            }
            return (com.github.benmanes.caffeine.cache.Weigher<Object, Object>) instance;
        }
    }

    /**
     * 将默认 {@code keyGenerator} Bean 接入 Spring Cache 切面。
     * <p>{@code CacheInterceptor} 的 keyGenerator 仅来源于 {@link CachingConfigurer}
     * （见 {@code AbstractCachingConfiguration#useCachingConfigurer}），没有
     * {@code CachingConfigurer} 时会静默回退到 {@code SimpleKeyGenerator}，
     * 导致 {@code <APP>:CACHE:...} 格式的 key 失效。
     * <p><b>设计说明</b>：{@code AbstractCachingConfiguration#setConfigurers} 在启动期
     * 收集容器内全部 {@link CachingConfigurer} Bean，数量大于 1 时直接抛
     * {@code IllegalStateException: 2 implementations of CachingConfigurer were found}。
     * {@code @ConditionalOnMissingBean} 在此场景不可靠——业务侧的 {@code CachingConfigurer}
     * 若来自独立自动装配（另一轮配置解析），本配置评估条件时尚未注册，条件误判为
     * 「不存在」从而照常装配，最终仍触发多实现冲突。因此本配置<b>无条件注册</b>，
     * 与业务侧自定义 {@code CachingConfigurer} 为「二选一」关系：
     * 业务侧接管时须整体关闭本模块自动装配（{@code cache.enabled=false}），
     * 详见 README「继承类 / 扩展点」。
     */
    @Configuration(proxyBeanMethods = false)
    public static class CacheCachingConfigurer implements CachingConfigurer {

        @Autowired
        @Qualifier("keyGenerator")
        private KeyGenerator keyGenerator;

        @Override
        public KeyGenerator keyGenerator() {
            return keyGenerator;
        }
    }

    /**
     * 组合 CacheManager：仅当 Hazelcast 与 Caffeine 同时在 classpath 时生效。
     * <p>主 = Hazelcast 分布式缓存，次 = Caffeine 本地缓存；读按主->次，写双写。
     */
    @Configuration
    @ConditionalOnClass({HazelcastInstance.class, Caffeine.class})
    public static class CompositeCacheConfiguration {

        /**
         * {@code @ConditionalOnClass} 只保证类在 classpath，不保证两个底层 CacheManager
         * 均已装配（各自还有其他装配条件）。用 {@link ObjectProvider} 判空：
         * 任一缺失时退化为存在的单级，避免 {@code NoSuchBeanDefinitionException} 启动失败。
         */
        @Bean
        @org.springframework.context.annotation.Primary
        @ConditionalOnMissingBean(name = "cacheManager")
        public CacheManager cacheManager(
                @Qualifier("hazelcastCacheManager") ObjectProvider<CacheManager> hazelcast,
                @Qualifier("caffeineCacheManager") ObjectProvider<CacheManager> caffeine) {
            CacheManager primary = hazelcast.getIfAvailable();
            CacheManager local = caffeine.getIfAvailable();
            if (primary == null || local == null) {
                // 单级退化：哪级存在用哪级
                return primary != null ? primary : local;
            }
            return new CompositeCacheManager(primary, local);
        }
    }

}
