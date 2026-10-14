/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.core.feign
 * File Name:GlobalLoadBalancerConfiguration
 * Date:2025/9/8 15:06
 */
package org.wymix.framework.core.feign;

import org.wymix.framework.core.feign.supports.DynamicServiceInstanceSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * ClassName: GlobalLoadBalancerConfiguration
 * Description: 
 * Date: 2025/9/8 15:06
 *
 * @author wang_yang
 */
public class GlobalLoadBalancerConfiguration {

    @Bean
    public ReactorServiceInstanceLoadBalancer customRoundRobinLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class), serviceId);
    }

    @Bean
    @ConditionalOnBean(DiscoveryClient.class)
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return new DynamicServiceInstanceSupplier(ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching().build(context));
    }

}
