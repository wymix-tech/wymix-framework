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

import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.NodeRegistry;
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
