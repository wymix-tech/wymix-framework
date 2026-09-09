package com.travelsky.base.cache.manager;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Set;
import java.util.stream.Collectors;
import com.travelsky.base.cache.properties.HazelcastCacheProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomHazelcastCacheManager 单元测试：
 * 验证 appName 通过构造器注入后的命名空间前缀行为（P0-2）。
 */
class CustomHazelcastCacheManagerTest {

    private HazelcastInstance hazelcastInstance;

    @AfterEach
    void tearDown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
    }

    @Test
    void appPrefixShouldBeUsedInNamespaceKey() {
        hazelcastInstance = Hazelcast.newHazelcastInstance(new Config());
        CustomHazelcastCacheManager manager = new CustomHazelcastCacheManager("my-app");
        HazelcastCacheProperties properties = new HazelcastCacheProperties();
        manager.setHazelcastInstance(hazelcastInstance);
        manager.setHazelcastCacheProperties(properties);

        manager.getCache("user");
        Set<String> mapNames = hazelcastInstance.getDistributedObjects().stream()
                .filter(o -> o instanceof IMap)
                .map(o -> ((IMap<?, ?>) o).getName())
                .collect(Collectors.toSet());
        // 未声明命名空间回退 default，key 为 <appPrefix>:default
        assertTrue(mapNames.contains("my-app:default"));
        // 不应出现 null 前缀的脏 key
        assertTrue(mapNames.stream().noneMatch(n -> n.startsWith("null")));
    }

    @Test
    void blankAppPrefixShouldFallbackToDefault() {
        CustomHazelcastCacheManager manager = new CustomHazelcastCacheManager("");
        hazelcastInstance = Hazelcast.newHazelcastInstance(new Config());
        HazelcastCacheProperties properties = new HazelcastCacheProperties();
        manager.setHazelcastInstance(hazelcastInstance);
        manager.setHazelcastCacheProperties(properties);

        manager.getCache("user");
        Set<String> mapNames = hazelcastInstance.getDistributedObjects().stream()
                .filter(o -> o instanceof IMap)
                .map(o -> ((IMap<?, ?>) o).getName())
                .collect(Collectors.toSet());
        assertTrue(mapNames.contains("default:default"));
    }
}
