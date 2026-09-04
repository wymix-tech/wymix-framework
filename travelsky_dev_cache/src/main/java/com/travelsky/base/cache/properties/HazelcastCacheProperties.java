package com.travelsky.base.cache.properties;

import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MaxSizePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.hazelcast.cache")
public class HazelcastCacheProperties {

    public String publicAddress = "127.0.0.1:5701";
    private String instanceName;
    private DeployMode mode = DeployMode.SINGLETON;
    private boolean tcpIpEnabled = true;
    private List<String> members = Collections.singletonList("127.0.0.1");
    private Map<String, ExpiredConfig> namespace;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public DeployMode getMode() {
        return mode;
    }

    public void setMode(DeployMode mode) {
        this.mode = mode;
    }

    public boolean isTcpIpEnabled() {
        return tcpIpEnabled;
    }

    public void setTcpIpEnabled(boolean tcpIpEnabled) {
        this.tcpIpEnabled = tcpIpEnabled;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public void setPublicAddress(String publicAddress) {
        this.publicAddress = publicAddress;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public Map<String, ExpiredConfig> getNamespace() {
        return namespace;
    }

    public void setNamespace(Map<String, ExpiredConfig> namespace) {
        this.namespace = namespace;
    }

    public enum DeployMode {
        SINGLETON, CLUSTER, CLIENT,
    }

    public static class ExpiredConfig {

        private int timeToLiveInSeconds = 86400;

        private int evictionPolicy = EvictionPolicy.LRU.getId();

        private int maxSizePolicy = MaxSizePolicy.PER_NODE.getId();

        private int size = 10000;

        private int keyTimeToLiveInSeconds = 86400;

        public int getTimeToLiveInSeconds() {
            return timeToLiveInSeconds;
        }

        public void setTimeToLiveInSeconds(int timeToLiveInSeconds) {
            this.timeToLiveInSeconds = timeToLiveInSeconds;
        }

        public int getEvictionPolicy() {
            return evictionPolicy;
        }

        public void setEvictionPolicy(int evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
        }

        public int getMaxSizePolicy() {
            return maxSizePolicy;
        }

        public void setMaxSizePolicy(int maxSizePolicy) {
            this.maxSizePolicy = maxSizePolicy;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getKeyTimeToLiveInSeconds() {
            return keyTimeToLiveInSeconds;
        }

        public void setKeyTimeToLiveInSeconds(int keyTimeToLiveInSeconds) {
            this.keyTimeToLiveInSeconds = keyTimeToLiveInSeconds;
        }
    }
}
