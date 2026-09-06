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
import com.travelsky.base.cache.manager.CustomHazelcastCacheManager;
import com.travelsky.base.cache.properties.CaffeineCacheProperties;
import com.travelsky.base.cache.properties.HazelcastCacheProperties;
import com.travelsky.base.utils.json.JsonUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
public class CacheManagerConfiguration implements CachingConfigurer {

    private static final String SPLITTER = ":";

    @Value("${spring.application.name:default}")
    private String appName;

    @Bean
    @Override
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

        public HazelcastInstance hazelcastInstance() {
            return Hazelcast.newHazelcastInstance(buildConfig());
        }

        private Config buildConfig() {
            Config config = new Config(hazelcastCacheProperties.getInstanceName());
            config.getNetworkConfig().getRestApiConfig().setEnabled(true);
            if (hazelcastCacheProperties.getMode() == HazelcastCacheProperties.DeployMode.SINGLETON) {
                config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
                TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
                tcpIpConfig.setEnabled(true);
                for (String member : hazelcastCacheProperties.getMembers()) {
                    tcpIpConfig.addMember(member);
                }
                config.setProperty("hazelcast.local.public.address", hazelcastCacheProperties.getPublicAddress());
            } else {
                JoinConfig joinConfig = config.getNetworkConfig().getJoin();
                joinConfig.getMulticastConfig().setEnabled(false);
                TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
                tcpIpConfig.setEnabled(true);
                for (String member : hazelcastCacheProperties.getMembers()) {
                    tcpIpConfig.addMember(member);
                }
                config.setProperty("hazelcast.local.public.address", hazelcastCacheProperties.getPublicAddress());
            }

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
        public CacheManager cacheManager() {
            CustomHazelcastCacheManager cacheManager = new CustomHazelcastCacheManager();
            cacheManager.setHazelcastInstance(hazelcastInstance());
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
        public CacheManager cacheManager() {
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

        private Caffeine<Object, Object> buildeCaffeine(CaffeineCacheProperties.CaffeineConfig config) {
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
            caffeine.initialCapacity(config.getInitialCapacity());
            if (config.getMaximumSize() > 0) {
                caffeine.maximumSize(config.getMaximumSize());
            }
            if (config.getMaximumWeight() > 0) {
                caffeine.maximumWeight(config.getMaximumWeight());
            }
            if (config.getPolicy() == CaffeineCacheProperties.Policy.AFTER_WRITE) {
                caffeine.expireAfterWrite(config.getExpireInSec(), TimeUnit.SECONDS);
            } else {
                caffeine.expireAfterAccess(config.getExpireInSec(), TimeUnit.SECONDS);
            }
            if (config.getRefreshAfterWriterInSec() > 0) {
                caffeine.refreshAfterWrite(config.getRefreshAfterWriterInSec(), TimeUnit.SECONDS);
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
    }

}
