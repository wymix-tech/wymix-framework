/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.enums
 * File Name:SystemTopic
 * Date:2025/9/9 9:28
 */
package com.travelsky.base.discovery.enums;

/**
 * ClassName: SystemTopic
 * Description:
 * Date: 2025/9/9 9:28
 *
 * @author wang_yang
 */
public enum SystemTopic {

    OFFLINE("service-cluster-instance-offline");

    private String code;

    SystemTopic(String code) {
        this.code = code;
    }

    public String topic() {
        return this.code;
    }
}
