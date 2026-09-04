/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.endpoint
 * File Name:ElectionEndpoint
 * Date:2025/9/3 16:53
 */
package com.travelsky.base.discovery.endpoint;

import com.travelsky.base.discovery.supports.ElectionService;
import com.travelsky.base.discovery.supports.NodeRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

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

    @WriteOperation
    public String election(@Selector String oldLeaderName, @Selector String newLeaderName) {
        if (!String.format("%s#%s:%d", NodeRegistry.leader().getServiceName(), NodeRegistry.leader().getHost(), NodeRegistry.leader().getPort()).equals(newLeaderName)) {
            NodeRegistry.signOut(oldLeaderName);
            electionService.electLeader(false);
        }
        return String.format("%s#%s:%d", NodeRegistry.leader().getServiceName(), NodeRegistry.leader().getHost(), NodeRegistry.leader().getPort());
    }

}
