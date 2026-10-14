/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.endpoint
 * File Name:NodeRefreshEndpoint
 * Date:2025/9/9 14:38
 */
package org.wymix.framework.discovery.endpoint;

import org.wymix.framework.discovery.supports.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * ClassName: NodeRefreshEndpoint
 * Description: 按配置重新同步集群节点
 * Date: 2025/9/9 14:38
 *
 * @author wang_yang
 */
@Endpoint(id = "nodeRefresh")
public class NodeRefreshEndpoint {

    @Autowired
    private NodeService nodeService;

    /**
     * 重新按 {@code decentralization.discovery.service.<svc>} 配置登记节点。
     *
     * <p>模块不做自动发现，节点只来自配置与其它实例的主动注册。当两端都已启动、
     * 却因启动时地址不通而漏登记时，用本端点补全；已登记的地址会跳过，可安全重复调用。
     *
     * @return 同步结果摘要（新登记数、失败数、已知节点数、当前 Leader）
     */
    @WriteOperation
    public String refresh() {
        return nodeService.syncConfiguredNodes();
    }

}
