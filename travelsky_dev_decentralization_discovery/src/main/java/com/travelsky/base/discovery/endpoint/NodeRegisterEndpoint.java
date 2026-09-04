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

import com.travelsky.base.core.feign.supports.NodeInfo;
import com.travelsky.base.discovery.supports.ElectionService;
import com.travelsky.base.discovery.supports.NodeRegistry;
import com.travelsky.base.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * ClassName: ElectionEndpoint
 * Description:
 * Date: 2025/9/3 16:53
 *
 * @author wang_yang
 */
@Endpoint(id = "nodeRegister")
public class NodeRegisterEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegisterEndpoint.class);
    @Autowired
    private ElectionService electionService;

    @WriteOperation
    public String register(String nodes) {
        NodeInfo nodeInfo = JsonUtils.jsonToEntity(NodeInfo.class, nodes);
        nodeInfo.setSelf(false);
        nodeInfo.setLeader(false);
        NodeRegistry.signUp(nodeInfo);
        electionService.electLeader(true);
        return JsonUtils.entityToJson(NodeRegistry.self());
    }

}
