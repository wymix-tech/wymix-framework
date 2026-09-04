/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.supports
 * File Name:HeartbeatDTO
 * Date:2025/9/4 14:31
 */
package com.travelsky.base.discovery.supports;

import java.io.Serializable;

/**
 * ClassName: HeartbeatDTO
 * Description:
 * Date: 2025/9/4 14:31
 *
 * @author wang_yang
 */
public class HeartbeatDTO implements Serializable {

    private String nodes;

    public HeartbeatDTO() {
    }

    public HeartbeatDTO(String nodes) {
        this.nodes = nodes;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }
}
