/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.endpoint
 * File Name:ElectionEndpoint
 * Date:2025/9/3 16:53
 */
package org.wymix.framework.discovery.endpoint;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.NodeRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.Objects;

/**
 * ClassName: ElectionEndpoint
 * Description:
 * Date: 2025/9/3 16:53
 *
 * @author wang_yang
 */
@Endpoint(id = "election")
public class ElectionEndpoint {

    @Autowired
    private ElectionService electionService;

    /**
     * 触发一轮重新选举。
     *
     * <p>调用方把「自己认知的旧 Leader」与「提议的新 Leader」一起传来；只有当本实例认知的现任
     * Leader 与提议不一致时才真正重选，避免同一批广播把集群反复拉进选举。
     *
     * @param oldLeaderName 旧 Leader 实例名（{@code serviceName#host:port}）
     * @param newLeaderName 提议的新 Leader 实例名
     * @return 本实例选举后的 Leader 实例名；无可用节点时为 null
     */
    @WriteOperation
    public String election(@Selector String oldLeaderName, @Selector String newLeaderName) {
        NodeInfo currentLeader = NodeRegistry.leader();
        String currentLeaderName = currentLeader == null ? null : currentLeader.instanceName();
        if (!Objects.equals(currentLeaderName, newLeaderName)) {
            NodeRegistry.signOut(oldLeaderName);
            electionService.electLeader(false);
        }
        NodeInfo elected = NodeRegistry.leader();
        return elected == null ? null : elected.instanceName();
    }

}
