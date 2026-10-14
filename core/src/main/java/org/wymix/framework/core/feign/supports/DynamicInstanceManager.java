/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:ams_airport_license_parent
 * Package Name:com.wymix.plc.ams.configuration.dynamic
 * File Name:DynamicInstanceManager
 * Date:2025/9/8 11:05
 */
package org.wymix.framework.core.feign.supports;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: DynamicInstanceManager
 * Description: 
 * Date: 2025/9/8 11:05
 *
 * @author wang_yang
 */
public class DynamicInstanceManager {

    /**
     * 动态添加一个服务实例
     */
    public static void addServiceInstance(String serviceId, String host, int port) {
        ServiceInstance instance = ServiceInstanceBuilder.createInstance(serviceId, host, port, false);
        DynamicServiceInstanceSupplier.addInstance(serviceId, instance);
    }

    /**
     * 设置一组实例（覆盖）
     */
    public static void setServiceInstances(String serviceId, List<NodeInfo> instances) {
        List<ServiceInstance> serviceInstances = instances.stream()
                .map(info -> ServiceInstanceBuilder.createInstance(serviceId, info.getHost(), info.getPort(), false))
                .toList();
        DynamicServiceInstanceSupplier.setInstances(serviceId, serviceInstances);
    }


    public static void removeServiceInstance(String serviceId, NodeInfo nodeInfo) {
        DynamicServiceInstanceSupplier.removeInstance(serviceId, ServiceInstanceBuilder.createInstance(serviceId, nodeInfo.getHost(), nodeInfo.getPort(), false));
    }

    /**
     * 清除某个服务的所有实例
     */
    public static void clearServiceInstances(String serviceId) {
        DynamicServiceInstanceSupplier.clearInstances(serviceId);
    }


}
