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
     * <p>不再通过实现 {@code CachingConfigurer} 暴露，避免业务侧自定义
     * {@code CachingConfigurer} 时触发多实现冲突；业务侧可直接声明同名
     * {@code keyGenerator} Bean 覆盖本默认实现。
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
     * 组合 CacheManager：仅当 Hazelcast 与 Caffeine 同时在 classpath 时生效。
     * <p>主 = Hazelcast 分布式缓存，次 = Caffeine 本地缓存；读按主->次，写双写。
     */
    @Configuration
    @ConditionalOnClass({HazelcastInstance.class, Caffeine.class})
    public static class CompositeCacheConfiguration {

        @Autowired
        @Qualifier("hazelcastCacheManager")
        private CacheManager hazelcastCacheManager;

        @Autowired
        @Qualifier("caffeineCacheManager")
        private CacheManager caffeineCacheManager;

        @Bean
        @org.springframework.context.annotation.Primary
        @ConditionalOnMissingBean(name = "cacheManager")
        public CacheManager cacheManager() {
            return new CompositeCacheManager(hazelcastCacheManager, caffeineCacheManager);
        }
    }

}
