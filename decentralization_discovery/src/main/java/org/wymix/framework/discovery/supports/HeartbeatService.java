/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:HeartbeatService
 * Date:2025/9/4 13:35
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.HealthStatus;
import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.broadcast.SelfServiceClusterBroadcast;
import org.wymix.framework.discovery.enums.SystemTopic;
import org.wymix.framework.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClassName: HeartbeatService
 * Description:
 * Date: 2025/9/4 13:35
 *
 * @author wang_yang
 */
public class HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);
    private final AtomicInteger cycle = new AtomicInteger(0);
    private ScheduledExecutorService executor;
    @Autowired
    private DiscoveryFeignClient discoveryFeignClient;
    @Autowired
    private DynamicUrlTarget<DiscoveryFeignClient> discoveryFeignClientHardCodedTarget;
    @Autowired
    private ElectionService electionService;
    @Autowired
    private SelfServiceClusterBroadcast selfServiceClusterBroadcast;
    @Value("${decentralization.discovery.heartbeat.periodInSeconds:30}")
    private int period;

    @PostConstruct
    public void startHeartbeatSchedule() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    executor = Executors.newSingleThreadScheduledExecutor();
                    executor.scheduleAtFixedRate(this::heartbeat, period, period, TimeUnit.SECONDS);
                }
            }
        }
    }

    private void heartbeat() {
        heartbeat0();
    }

    public boolean heartbeat0() {
        NodeInfo self = NodeRegistry.self();
        if (self == null) {
            // 自身节点尚未登记（启动未完成或注册表被重置），本轮无心跳可发
            return true;
        }
        List<NodeInfo> nodeInfos = NodeRegistry.nodes().get(self.getServiceName());
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (NodeRegistry.self().isLeader()) {
            boolean flag = true;
            for (NodeInfo nodeInfo : nodeInfos) {
                if (nodeInfo.isSelf()) {
                    nodeInfo.setLastSeen(now);
                    nodeInfo.setLastSeenSuccess(now);
                    continue;
                }
                try {
                    nodeInfo.setLastSeen(now);
                    discoveryFeignClientHardCodedTarget.setUrl(nodeInfo.url());
                    discoveryFeignClient.heartbeat(new HeartbeatDTO(JsonUtils.entityToJson(NodeRegistry.nodes())));
                    nodeInfo.setStatus(HealthStatus.UP);
                    nodeInfo.setLastSeenSuccess(now);
                    flag = true && flag;
                } catch (Exception e) {
                    long duration = Duration.between(nodeInfo.getLastSeenSuccess(), now).toMillis() / (period * 1000);
                    if (duration > 1.2 && duration < 2.2) {
                        nodeInfo.setStatus(HealthStatus.ERROR);
                        nodeInfo.setBrokenTimes(nodeInfo.getBrokenTimes() + 1);
                    } else if (duration >= 2.2) {
                        nodeInfo.setStatus(HealthStatus.DOWN);
                    }
                    nodeInfo.setLossTime(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - nodeInfo.getLastSeenSuccess().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    flag = false && flag;
                }
            }
            return flag;
        } else {
            NodeInfo leader = NodeRegistry.leader();
            if (leader == null) {
                // 集群无主（首次启动尚未选出，或 Leader 引用丢失），立即补一次选举，
                // 否则本实例会一直走「探测 Leader」分支而永远等不到 Leader
                electionService.electLeader(false);
                return true;
            }
            long duration = Duration.between(leader.getLastSeenSuccess(), now).toMillis() / (period * 1000);
            if (duration > 1.2 && duration < 2.2) {
                discoveryFeignClientHardCodedTarget.setUrl(leader.url());
                try {
                    discoveryFeignClient.checkLeaderStatus();
                    leader.setLastSeen(now);
                    leader.setLastSeenSuccess(now);
                } catch (Exception e) {
                    leader.setStatus(HealthStatus.ERROR);
                }
            } else if (duration >= 2.2) {
                discoveryFeignClientHardCodedTarget.setUrl(leader.url());
                try {
                    discoveryFeignClient.checkLeaderStatus();
                    leader.setLastSeen(now);
                    leader.setLastSeenSuccess(now);
                } catch (Exception e) {
                    leader.setStatus(HealthStatus.DOWN);
                    String serviceName = leader.instanceName();
                    NodeRegistry.signOut(serviceName);
                    electionService.electLeader(false);
                    try {
                        selfServiceClusterBroadcast.sendSync(SystemTopic.OFFLINE.topic(), serviceName, false);
                    } catch (ExecutionException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (InterruptedException ex) {
                        log.error(ex.getMessage(), ex);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return true;
    }


}
