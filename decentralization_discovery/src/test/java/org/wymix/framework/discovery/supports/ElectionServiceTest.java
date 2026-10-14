package org.wymix.framework.discovery.supports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.wymix.framework.core.feign.supports.HealthStatus;
import org.wymix.framework.core.feign.supports.NodeInfo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leader 选举的确定性与故障重选验证。
 *
 * <p>去中心化发现里每个实例各自计算 Leader，测试的重点不是「选出一个 Leader」，
 * 而是「<b>所有实例都选出同一个 Leader</b>」——即结果与节点登记顺序无关。
 */
class ElectionServiceTest {

    private static final String SERVICE = "svc";

    /**
     * 固定启动时间：所有节点用同一个 upTime，得分才会完全相同，从而触发平局决胜逻辑。
     */
    private static final LocalDateTime UP_TIME = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    private ElectionService electionService;

    @BeforeEach
    void setUp() {
        NodeRegistry.clear();
        electionService = new ElectionService();
        ReflectionTestUtils.setField(electionService, "appName", SERVICE);
    }

    @AfterEach
    void tearDown() {
        NodeRegistry.clear();
    }

    @Test
    @DisplayName("无节点时跳过选举，不抛异常也不产生 Leader")
    void noNodeShouldSkipElectionGracefully() {
        assertNull(electionService.electLeader(true));
        assertNull(electionService.electLeader(false));
        assertNull(NodeRegistry.leader());
    }

    @Test
    @DisplayName("仅一个节点时由它担任 Leader")
    void singleNodeShouldElectItself() {
        NodeInfo self = node("10.0.0.1", 8080, true);
        NodeRegistry.signUp(self);

        assertSame(self, electionService.electLeader(true));
        assertSame(self, NodeRegistry.leader());
        assertTrue(self.isLeader());
    }

    @Test
    @DisplayName("得分相同时按实例名字典序决胜，且与登记顺序无关")
    void tieShouldBeBrokenByInstanceNameRegardlessOfRegistrationOrder() {
        NodeInfo a = node("10.0.0.3", 8080, false);
        NodeInfo b = node("10.0.0.2", 8080, false);
        NodeInfo c = node("10.0.0.1", 8080, false);

        // 前置条件：三者得分确实相同，本用例才是在验证平局决胜
        assertEquals(a.priority(3), b.priority(3));
        assertEquals(b.priority(3), c.priority(3));

        NodeInfo first = electInOrder(a, b, c);
        NodeInfo second = electInOrder(c, b, a);
        NodeInfo third = electInOrder(b, a, c);

        // 字典序最小者胜：svc#10.0.0.1:8080
        assertEquals("svc#10.0.0.1:8080", first.instanceName());
        assertEquals(first.instanceName(), second.instanceName());
        assertEquals(first.instanceName(), third.instanceName());
    }

    @Test
    @DisplayName("得分不同时由得分最高者胜出")
    void highestScoreShouldWin() {
        NodeInfo stable = node("10.0.0.9", 8080, false);
        NodeInfo flaky = node("10.0.0.1", 8080, false);
        flaky.setBrokenTimes(30);

        assertTrue(stable.priority(2) > flaky.priority(2), "失联次数多的节点得分应更低");

        assertSame(stable, electInOrder(flaky, stable));
        assertSame(stable, electInOrder(stable, flaky));
    }

    @Test
    @DisplayName("常规选举保持现任 Leader；Leader 失效重选时才让位给得分更高者")
    void upElectionShouldKeepCurrentLeaderUntilItFails() {
        NodeInfo stable = node("10.0.0.9", 8080, false);
        NodeInfo flaky = node("10.0.0.1", 8080, false);
        flaky.setBrokenTimes(30);

        NodeRegistry.signUp(flaky);
        NodeRegistry.signUp(stable);
        // 模拟 flaky 此前已被选为 Leader
        NodeRegistry.setLeader(flaky);

        // 新节点加入触发的常规选举：现任仍健康就在候选列表里，不换人
        assertSame(flaky, electionService.electLeader(true));
        assertTrue(flaky.isLeader());
        assertFalse(stable.isLeader());

        // Leader 失效后的重选：不做粘性判断，由得分更高的节点上位
        assertSame(stable, electionService.electLeader(false));
        assertFalse(flaky.isLeader());
        assertTrue(stable.isLeader());
    }

    @Test
    @DisplayName("重选时剔除已判定 DOWN 的节点")
    void failedElectionShouldSkipDownNode() {
        NodeInfo healthy = node("10.0.0.2", 8080, false);
        NodeInfo dead = node("10.0.0.1", 8080, false);
        dead.setStatus(HealthStatus.DOWN);

        // dead 的实例名字典序更小，若未剔除 DOWN 节点，决胜时本应选中它
        assertSame(healthy, electInOrder(dead, healthy));
    }

    @Test
    @DisplayName("全部节点 DOWN 时仍产出一个 Leader，不让集群无主")
    void allDownShouldStillProduceLeader() {
        NodeInfo a = node("10.0.0.1", 8080, false);
        NodeInfo b = node("10.0.0.2", 8080, false);
        a.setStatus(HealthStatus.DOWN);
        b.setStatus(HealthStatus.DOWN);

        NodeInfo leader = electInOrder(a, b);
        assertEquals("svc#10.0.0.1:8080", leader.instanceName());
    }

    @Test
    @DisplayName("重复设置同一 Leader 不产生 isLeader=false 的中间态")
    void reappointingSameLeaderShouldStayLeader() {
        NodeInfo self = node("10.0.0.1", 8080, true);
        NodeRegistry.signUp(self);
        NodeRegistry.setLeader(self);
        assertTrue(self.isLeader());

        NodeRegistry.setLeader(self);
        assertTrue(self.isLeader(), "连任不应出现「Leader 存在却自称不是 Leader」的状态");
        assertSame(self, NodeRegistry.leader());
    }

    @Test
    @DisplayName("更换 Leader 时清除旧 Leader 标记")
    void switchingLeaderShouldClearPreviousFlag() {
        NodeInfo a = node("10.0.0.1", 8080, false);
        NodeInfo b = node("10.0.0.2", 8080, false);

        NodeRegistry.setLeader(a);
        assertTrue(a.isLeader());

        NodeRegistry.setLeader(b);
        assertFalse(a.isLeader());
        assertTrue(b.isLeader());
    }

    @Test
    @DisplayName("同端口不同主机的节点都应登记，不能互相覆盖")
    void samePortDifferentHostShouldBothBeRegistered() {
        NodeInfo a = node("10.0.0.1", 8080, false);
        NodeInfo b = node("10.0.0.2", 8080, false);

        NodeRegistry.signUp(a);
        NodeRegistry.signUp(b);

        assertEquals(2, NodeRegistry.nodes().get(SERVICE).size());
    }

    /**
     * 按给定顺序登记节点后触发一次「失效重选」（不带 Leader 粘性），返回选出的 Leader。
     */
    private NodeInfo electInOrder(NodeInfo... nodes) {
        NodeRegistry.clear();
        for (NodeInfo node : nodes) {
            NodeRegistry.signUp(node);
        }
        return electionService.electLeader(false);
    }

    private static NodeInfo node(String host, int port, boolean self) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setNamespace("test");
        nodeInfo.setHost(host);
        nodeInfo.setPort(port);
        nodeInfo.setServiceName(SERVICE);
        nodeInfo.setStatus(HealthStatus.UP);
        nodeInfo.setUpTime(UP_TIME);
        nodeInfo.setLastSeen(LocalDateTime.now());
        nodeInfo.setLastSeenSuccess(LocalDateTime.now());
        nodeInfo.setSelf(self);
        nodeInfo.setLeader(false);
        nodeInfo.setContextPath("");
        return nodeInfo;
    }
}
