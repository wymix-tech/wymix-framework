/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:ams_airport_license_parent
 * Package Name:com.travelsky.plc.ams.configuration.dynamic
 * File Name:DynamicServiceInstanceSupplier
 * Date:2025/9/8 11:01
 */
package com.travelsky.base.core.feign.supports;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClassName: DynamicServiceInstanceSupplier
 * Description:
 * Date: 2025/9/8 11:01
 *
 * @author wang_yang
 */
public class DynamicServiceInstanceSupplier implements ServiceInstanceListSupplier {
    private static final Map<String, List<ServiceInstance>> DYNAMIC_INSTANCES = new ConcurrentHashMap<>();

    private final ServiceInstanceListSupplier serviceInstanceListSupplier;

    public DynamicServiceInstanceSupplier(ServiceInstanceListSupplier serviceInstanceListSupplier) {
        this.serviceInstanceListSupplier = serviceInstanceListSupplier;
    }

    public static void addInstance(String serviceId, ServiceInstance instance) {
        DYNAMIC_INSTANCES.computeIfAbsent(serviceId, k -> new CopyOnWriteArrayList<>()).add(instance);
    }

    public static void removeInstance(String serviceId, ServiceInstance instance) {
        DYNAMIC_INSTANCES.computeIfPresent(serviceId, (k, list) -> {
            list.removeIf(ins -> ins.getHost().equals(instance.getHost()) && ins.getPort() == instance.getPort());
            return list.isEmpty() ? null : list;
        });
    }

    // ========== 动态控制 API ========== //

    public static void setInstances(String serviceId, List<ServiceInstance> instances) {
        DYNAMIC_INSTANCES.put(serviceId, new CopyOnWriteArrayList<>(instances));
    }

    public static void clearInstances(String serviceId) {
        DYNAMIC_INSTANCES.remove(serviceId);
    }

    public static List<ServiceInstance> getInstances(String serviceId) {
        return DYNAMIC_INSTANCES.getOrDefault(serviceId, Collections.emptyList());
    }

    @Override
    public String getServiceId() {
        return serviceInstanceListSupplier.getServiceId();
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        if (DYNAMIC_INSTANCES.get(getServiceId()) != null) {
            return Flux.just(DYNAMIC_INSTANCES.get(getServiceId()));
        }
        return serviceInstanceListSupplier.get();
    }
}
