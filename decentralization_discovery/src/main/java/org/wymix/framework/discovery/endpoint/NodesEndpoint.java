/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.endpoint
 * File Name:NodesEndpoint
 * Date:2025/9/9 14:38
 */
package org.wymix.framework.discovery.endpoint;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.supports.NodeRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: NodesEndpoint
 * Description: 集群节点视图
 * Date: 2025/9/9 14:38
 *
 * @author wang_yang
 */
@Endpoint(id = "nodes")
public class NodesEndpoint {

    /**
     * 查看当前已知节点、自身与 Leader。
     *
     * <p>模块不做自动发现，集群成员完全由配置决定，因此需要一个只读端点来核对
     * 「配置是否都登记上了」以及「各实例认知的 Leader 是否一致」。
     *
     * @return 节点视图
     */
    @ReadOperation
    public Map<String, Object> nodes() {
        NodeInfo self = NodeRegistry.self();
        NodeInfo leader = NodeRegistry.leader();

        List<Map<String, Object>> nodes = new ArrayList<>();
        NodeRegistry.nodes().forEach((service, list) -> list.forEach(node -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("service", service);
            item.put("instance", node.instanceName());
            item.put("url", node.url());
            item.put("status", node.getStatus());
            item.put("leader", node.isLeader());
            item.put("self", node.isSelf());
            item.put("brokenTimes", node.getBrokenTimes());
            item.put("upTime", node.getUpTime());
            item.put("lastSeenSuccess", node.getLastSeenSuccess());
            nodes.add(item);
        }));

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("self", self == null ? null : self.instanceName());
        view.put("leader", leader == null ? null : leader.instanceName());
        view.put("size", nodes.size());
        view.put("nodes", nodes);
        return view;
    }

}
