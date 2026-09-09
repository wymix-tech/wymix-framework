package com.travelsky.base.cache.manager;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.cache.HazelcastCache;
import com.travelsky.base.cache.properties.HazelcastCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomHazelcastCacheManager implements CacheManager {

    /**
     * Property name for hazelcast spring-cache related properties.
     */
    public static final String CACHE_PROP = "spring.hazelcast.cache.prop";
    private static final String NAMESPACE = "%s:%s";
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
    /**
     * 应用名前缀，参与命名空间拼接（{@code <appPrefix>:<cacheName>}）。
     * 由装配方（{@code CacheManagerConfiguration}）显式传入，
     * 未配置 {@code spring.application.name} 时回退为 {@code default}。
     */
    private final String appPrefix;
    private ConcurrentMap<String, String> formatNames = new ConcurrentHashMap<>();
    private HazelcastInstance hazelcastInstance;

    private HazelcastCacheProperties hazelcastCacheProperties;

    public CustomHazelcastCacheManager(String appPrefix) {
        this.appPrefix = appPrefix == null || appPrefix.isBlank() ? "default" : appPrefix;
    }

    public HazelcastCacheProperties getHazelcastCacheProperties() {
        return hazelcastCacheProperties;
    }

    public void setHazelcastCacheProperties(HazelcastCacheProperties hazelcastCacheProperties) {
        this.hazelcastCacheProperties = hazelcastCacheProperties;
    }

    @Override
    public Cache getCache(String name) {
        if (hazelcastCacheProperties.getNamespace() == null
                || !hazelcastCacheProperties.getNamespace().containsKey(name)) {
            name = "default";
        }
        String key = formatNames.computeIfAbsent(name, (k) -> String.format(NAMESPACE, appPrefix, k));
        Cache cache = caches.get(key);
        if (cache == null) {
            IMap<Object, Object> map = hazelcastInstance.getMap(key);
            cache = new HazelcastCache(map);
            long cacheTimeout = calculateCacheReadTimeout(name);
            ((HazelcastCache) cache).setReadTimeout(cacheTimeout);
            Cache currentCache = caches.putIfAbsent(key, cache);
            if (currentCache != null) {
                cache = currentCache;
            }
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> cacheNames = new HashSet<String>();
        Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();
        for (DistributedObject distributedObject : distributedObjects) {
            if (distributedObject instanceof IMap) {
                IMap<?, ?> map = (IMap) distributedObject;
                cacheNames.add(map.getName());
            }
        }
        return cacheNames;
    }

    private long calculateCacheReadTimeout(String name) {
        if (hazelcastCacheProperties.getNamespace() == null || !hazelcastCacheProperties.getNamespace().containsKey(name)) {
            return 3600;
        }
        Integer timeout = hazelcastCacheProperties.getNamespace().get(name).getKeyTimeToLiveInSeconds();
        return timeout == null ? 3600 : timeout;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }
}
