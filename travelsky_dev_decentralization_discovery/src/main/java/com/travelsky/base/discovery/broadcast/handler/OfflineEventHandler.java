/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.broadcast.handler
 * File Name:OfflineEventHandler
 * Date:2025/9/9 9:46
 */
package com.travelsky.base.discovery.broadcast.handler;

import com.travelsky.base.discovery.enums.SystemTopic;
import com.travelsky.base.discovery.supports.ElectionService;
import com.travelsky.base.discovery.supports.NodeRegistry;
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
