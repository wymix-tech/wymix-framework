/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.supports
 * File Name:DiscoveryFeignClient
 * Date:2025/9/4 14:23
 */
package com.travelsky.base.discovery.supports;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * ClassName: DiscoveryFeignClient
 * Description:
 * Date: 2025/9/4 14:23
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

    @PostMapping(value = "/actuator/nodeRegister")
    String register(HeartbeatDTO dto);
}
