/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.endpoint
 * File Name:DecentralizedDiscoverEndpoint
 * Date:2025/9/3 16:09
 */
package org.wymix.framework.discovery.endpoint;

import org.wymix.framework.discovery.supports.NodeRefresher;
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

    /**
     * 接收 Leader 心跳广播。
     *
     * <p>参数名必须是 {@code nodes}：actuator 按参数名绑定请求体，而调用方
     * {@code DiscoveryFeignClient#heartbeat} 发送的是 {@link HeartbeatDTO}（字段 {@code nodes}）。
     */
    @WriteOperation
    public String heartbeat(String nodes) {
        NodeRefresher.refresh(nodes);
        return "ok";
    }

}
