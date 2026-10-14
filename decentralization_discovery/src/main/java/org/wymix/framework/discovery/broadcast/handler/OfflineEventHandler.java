/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.broadcast.handler
 * File Name:OfflineEventHandler
 * Date:2025/9/9 9:46
 */
package org.wymix.framework.discovery.broadcast.handler;

import org.wymix.framework.discovery.enums.SystemTopic;
import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.NodeRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ClassName: OfflineEventHandler
 * Description: 
 * Date: 2025/9/9 9:46
 *
 * @author wang_yang
 */
public class OfflineEventHandler implements EventHandler<String> {

    @Autowired
    private ElectionService electionService;

    @Override
    public String topic() {
        return SystemTopic.OFFLINE.topic();
    }

    @Override
    public void handle(String payload) {
        boolean isLeader = NodeRegistry.leader().instanceName().equals(payload);
        NodeRegistry.signOut(payload);
        if (isLeader) {
            electionService.electLeader(false);
        }
    }
}
