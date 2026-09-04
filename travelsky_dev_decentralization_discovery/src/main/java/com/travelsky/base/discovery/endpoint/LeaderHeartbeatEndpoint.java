/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.endpoint
 * File Name:DecentralizedDiscoverEndpoint
 * Date:2025/9/3 16:09
 */
package com.travelsky.base.discovery.endpoint;

import com.travelsky.base.discovery.supports.NodeRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * ClassName: DecentralizedDiscoverEndpoint
 * Description:
 * Date: 2025/9/3 16:09
 *
 * @author wang_yang
 */
@Endpoint(id = "leaderHeartbeat")
public class LeaderHeartbeatEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LeaderHeartbeatEndpoint.class);

    @WriteOperation
    public String heartbeat(String nodes) {
        NodeRefresher.refresh(nodes);
        return "ok";
    }

}
