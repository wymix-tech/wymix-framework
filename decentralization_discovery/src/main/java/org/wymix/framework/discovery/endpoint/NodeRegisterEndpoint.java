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
import org.wymix.framework.utils.json.JsonUtils;
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
