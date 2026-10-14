package org.wymix.framework.discovery.supports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wymix.framework.core.feign.supports.HealthStatus;
import org.wymix.framework.core.feign.supports.NodeInfo;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点注册表判重与查找验证。
 *
 * <p>模块改为配置驱动后，{@link NodeRegistry#find(String, int)} 是「按配置同步节点」时判重的依据，
 * 判错会导致每次同步都重复发起注册请求。
 */
class NodeRegistryTest {

    private static final String SERVICE = "svc";

    @BeforeEach
    void setUp() {
        NodeRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        NodeRegistry.clear();
    }

    @Test
    @DisplayName("按 host + port 命中已登记节点")
    void findShouldMatchByHostAndPort() {
        NodeInfo node = node("10.0.0.1", 8080);
        NodeRegistry.signUp(node);

        Optional<NodeInfo> found = NodeRegistry.find("10.0.0.1", 8080);
        assertTrue(found.isPresent());
        assertSame(node, found.get());
    }

    @Test
    @DisplayName("端口不同不算同一节点")
    void findShouldDistinguishPort() {
        NodeRegistry.signUp(node("10.0.0.1", 8080));

        assertFalse(NodeRegistry.find("10.0.0.1", 8081).isPresent());
    }

    @Test
    @DisplayName("主机不同不算同一节点")
    void findShouldDistinguishHost() {
        NodeRegistry.signUp(node("10.0.0.1", 8080));

        assertFalse(NodeRegistry.find("10.0.0.2", 8080).isPresent());
    }

    @Test
    @DisplayName("重复登记同一节点不会产生两条记录")
    void signUpSameNodeTwiceShouldKeepOne() {
        NodeRegistry.signUp(node("10.0.0.1", 8080));
        NodeRegistry.signUp(node("10.0.0.1", 8080));

        assertEquals(1, NodeRegistry.nodes().get(SERVICE).size());
    }

    @Test
    @DisplayName("clear 后节点与 Leader 一并清空")
    void clearShouldResetEverything() {
        NodeInfo node = node("10.0.0.1", 8080);
        NodeRegistry.signUp(node);
        NodeRegistry.setLeader(node);

        NodeRegistry.clear();

        assertEquals(0, NodeRegistry.size());
        assertFalse(NodeRegistry.find("10.0.0.1", 8080).isPresent());
        assertEquals(null, NodeRegistry.leader());
    }

    private static NodeInfo node(String host, int port) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setNamespace("test");
        nodeInfo.setHost(host);
        nodeInfo.setPort(port);
        nodeInfo.setServiceName(SERVICE);
        nodeInfo.setStatus(HealthStatus.UP);
        nodeInfo.setUpTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        nodeInfo.setLastSeen(LocalDateTime.now());
        nodeInfo.setLastSeenSuccess(LocalDateTime.now());
        nodeInfo.setContextPath("");
        return nodeInfo;
    }
    @org.junit.jupiter.api.Test void removesLoadBalancerInstancesWhenNodeLeaves() {
        NodeInfo node = new NodeInfo(); node.setServiceName("lifecycle"); node.setHost("127.0.0.3"); node.setPort(9090);
        NodeRegistry.signUp(node); NodeRegistry.signOut(node.instanceName());
        org.junit.jupiter.api.Assertions.assertTrue(org.wymix.framework.core.feign.supports.DynamicServiceInstanceSupplier.getInstances("lifecycle").isEmpty());
    }

}
