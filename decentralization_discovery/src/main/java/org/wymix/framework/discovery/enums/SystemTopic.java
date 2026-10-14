/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.enums
 * File Name:SystemTopic
 * Date:2025/9/9 9:28
 */
package org.wymix.framework.discovery.enums;

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
