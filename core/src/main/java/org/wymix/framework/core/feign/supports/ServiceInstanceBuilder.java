/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:ams_airport_license_parent
 * Package Name:com.wymix.plc.ams.configuration
 * File Name:ServiceInstanceBuilder
 * Date:2025/9/8 11:04
 */
package org.wymix.framework.core.feign.supports;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Map;

/**
 * ClassName: ServiceInstanceBuilder
 * Description:
 * Date: 2025/9/8 11:04
 *
 * @author wang_yang
 */
public class ServiceInstanceBuilder {

    public static ServiceInstance createInstance(String serviceId, String host, int port, boolean secure) {
        return new DefaultServiceInstance(
                serviceId + "-" + host + ":" + port, // instanceId
                serviceId,
                host,
                port,
                secure
        );
    }

    public static ServiceInstance createInstanceWithMetadata(String serviceId, String host, int port, Map<String, String> metadata) {
        DefaultServiceInstance instance = new DefaultServiceInstance(
                serviceId + "-" + host + ":" + port,
                serviceId,
                host,
                port,
                false
        );
        instance.getMetadata().putAll(metadata);
        return instance;
    }
}
