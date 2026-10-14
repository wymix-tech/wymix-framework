/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:ElectionService
 * Date:2025/9/4 10:28
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Comparator;
import java.util.List;

/**
 * ClassName: ElectionService
 * Description:
 * Date: 2025/9/4 10:28
 *
 * @author wang_yang
 */
public class ElectionService {

    private static final Logger logger = LoggerFactory.getLogger(ElectionService.class);
    @Value("${spring.application.name:default}")
    private String appName;


    public ElectionService() {
    }

    public synchronized void electLeader(boolean up) {
        List<NodeInfo> nodeInfos = NodeRegistry.nodes().get(this.appName);
        NodeInfo electedLeaderNode;

        if (nodeInfos.size() == 1 && nodeInfos.get(0).isSelf()) {
            electedLeaderNode = NodeRegistry.self();
            // 如果没有其他实例，自己就是 Leader
            if (!electedLeaderNode.isLeader()) {
                NodeRegistry.setLeader(electedLeaderNode);
            }
            logger.info("Service Cluster Leader is: {}", String.format("%s:%d", electedLeaderNode.getHost(), electedLeaderNode.getPort()));
            return;
        }


        if (up) {
            electedLeaderNode = nodeInfos.stream().sorted(Comparator.comparing(NodeInfo::instanceName)).max(Comparator.comparingDouble(NodeInfo::priorityForUp)).orElse(null);
        } else {
            electedLeaderNode = nodeInfos.stream().sorted(Comparator.comparing(NodeInfo::instanceName)).max(Comparator.comparingDouble(NodeInfo::priorityForDown)).orElse(null);
        }

        if (electedLeaderNode != null) {
            NodeRegistry.setLeader(electedLeaderNode);
        }

        logger.info("Service Cluster Leader is: {}", String.format("%s:%d", electedLeaderNode.getHost(), electedLeaderNode.getPort()));
    }

}
