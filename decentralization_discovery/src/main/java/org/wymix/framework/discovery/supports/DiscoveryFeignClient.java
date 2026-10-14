/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:DiscoveryFeignClient
 * Date:2025/9/4 14:23
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * ClassName: DiscoveryFeignClient
 * Description: 节点间通信接口
 * Date: 2025/9/4 14:23
 *
 * <p>返回类型必须与端点实际返回的 JSON 形状一致：{@code nodeRegister} 端点返回的是对端
 * {@link NodeInfo} 序列化出的 JSON <b>对象</b>，声明为 {@code String} 会让 Jackson 抛
 * {@code Cannot deserialize value of type java.lang.String from Object value}。
 *
 * @author wang_yang
 */
public interface DiscoveryFeignClient {

    @PostMapping(value = "/actuator/leaderHeartbeat", consumes = MediaType.APPLICATION_JSON_VALUE)
    String heartbeat(HeartbeatDTO dto);

    @GetMapping(value = "/actuator/checkLeaderStatus")
    boolean checkLeaderStatus();

    @PostMapping(value = "/actuator/election/{oldLeaderName}/{newLeaderName}")
    String election(@PathVariable("oldLeaderName") String oldLeaderName, @PathVariable("newLeaderName") String newLeaderName);

    @PostMapping(value = "/actuator/broadcast/{topic}/{className}")
    String broadcast(@PathVariable("topic") String topic, @PathVariable("className") String className, HeartbeatDTO dto);

    @PostMapping(value = "/actuator/nodeRegister", consumes = MediaType.APPLICATION_JSON_VALUE)
    NodeInfo register(HeartbeatDTO dto);
}
