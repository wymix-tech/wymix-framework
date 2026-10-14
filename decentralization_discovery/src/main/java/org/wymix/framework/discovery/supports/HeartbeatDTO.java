/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:HeartbeatDTO
 * Date:2025/9/4 14:31
 */
package org.wymix.framework.discovery.supports;

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
