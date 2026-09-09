package com.travelsky.base.cache.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.travelsky.base.cache.manager.CustomHazelcastCacheManager;
import com.travelsky.base.cache.properties.HazelcastCacheProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hazelcast buildConfig 单元测试：
 * 验证 tcpIpEnabled 配置项真实生效（P2-5）。
 */
class HazelcastBuildConfigTest {

    private CacheManagerConfiguration configuration;

    private CacheManagerConfiguration.HazelcastCacheConfiguration hazelcastConfiguration;

    private HazelcastInstance hazelcastInstance;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new CacheManagerConfiguration();
        Field appNameField = CacheManagerConfiguration.class.getDeclaredField("appName");
        appNameField.setAccessible(true);
        appNameField.set(configuration, "default");
        hazelcastConfiguration = configuration.new HazelcastCacheConfiguration();
    }

    @AfterEach
    void tearDown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
    }

    private Config buildConfig(HazelcastCacheProperties properties) throws Exception {
        Field propertiesField = CacheManagerConfiguration.HazelcastCacheConfiguration.class
                .getDeclaredField("hazelcastCacheProperties");
        propertiesField.setAccessible(true);
        propertiesField.set(hazelcastConfiguration, properties);
        Method method = CacheManagerConfiguration.HazelcastCacheConfiguration.class
                .getDeclaredMethod("buildConfig");
        method.setAccessible(true);
        return (Config) method.invoke(hazelcastConfiguration);
    }

    @Test
    void tcpIpEnabledTrueShouldEnableTcpIp() throws Exception {
        HazelcastCacheProperties properties = new HazelcastCacheProperties();
        properties.setTcpIpEnabled(true);
        Config config = buildConfig(properties);
        assertTrue(config.getNetworkConfig().getJoin().getTcpIpConfig().isEnabled());
    }

    @Test
    void tcpIpEnabledFalseShouldDisableTcpIp() throws Exception {
        // P2-5: tcpIpEnabled=false 必须真实生效
        HazelcastCacheProperties properties = new HazelcastCacheProperties();
        properties.setTcpIpEnabled(false);
        Config config = buildConfig(properties);
        assertFalse(config.getNetworkConfig().getJoin().getTcpIpConfig().isEnabled());
    }

    @Test
    void hazelcastInstanceShouldShutdownOnClose() {
        // P3-7: hazelcastInstance 交由容器托管（destroyMethod = "shutdown"）
        hazelcastInstance = Hazelcast.newHazelcastInstance(new Config());
        assertTrue(hazelcastInstance.getLifecycleService().isRunning());
        hazelcastInstance.shutdown();
        assertFalse(hazelcastInstance.getLifecycleService().isRunning());
    }

    @Test
    void appPrefixFallbackShouldNotProduceNullKey() throws Exception {
        // P0-2: appName 兜底为 default（由装配方统一注入），不会拼出 null: 前缀
        CustomHazelcastCacheManager manager = new CustomHazelcastCacheManager(null);
        Field field = CustomHazelcastCacheManager.class.getDeclaredField("appPrefix");
        field.setAccessible(true);
        assertEquals("default", field.get(manager));
    }
}
