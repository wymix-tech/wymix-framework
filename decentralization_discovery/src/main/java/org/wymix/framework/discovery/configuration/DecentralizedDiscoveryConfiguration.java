/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.configuration
 * File Name:DecentraDiscoveryConfiguration
 * Date:2025/9/3 10:44
 */
package org.wymix.framework.discovery.configuration;

import org.wymix.framework.core.configuration.CustomFeignConfiguration;
import org.wymix.framework.discovery.broadcast.SelfServiceClusterBroadcast;
import org.wymix.framework.discovery.broadcast.handler.OfflineEventHandler;
import org.wymix.framework.discovery.broadcast.listener.BroadcastListener;
import org.wymix.framework.discovery.endpoint.BroadcastEndpoint;
import org.wymix.framework.discovery.endpoint.CheckLeaderStatusEndpoint;
import org.wymix.framework.discovery.endpoint.ElectionEndpoint;
import org.wymix.framework.discovery.endpoint.LeaderHeartbeatEndpoint;
import org.wymix.framework.discovery.endpoint.NodeRegisterEndpoint;
import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;
import org.wymix.framework.discovery.supports.DiscoveryFeignClient;
import org.wymix.framework.discovery.supports.DynamicUrlTarget;
import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.HeartbeatService;
import org.wymix.framework.discovery.supports.JmDnsService;
import org.wymix.framework.discovery.supports.NodeService;
import feign.Client;
import feign.Feign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.jmdns.JmDNS;
import java.net.InetAddress;

/**
 * ClassName: DecentraDiscoveryConfiguration
 * Description:
 * Date: 2025/9/3 10:44
 *
 * @author wang_yang
 */
@Configuration
@EnableConfigurationProperties(DecentralizedDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "decentralization.discovery", name = "enable", havingValue = "true")
public class DecentralizedDiscoveryConfiguration extends CustomFeignConfiguration {

    @Autowired
    private DecentralizedDiscoveryProperties decentralizedDiscoveryProperties;


    @Bean
    public ElectionService electionService() {
        ElectionService electionService = new ElectionService();
        return electionService;
    }

    @Bean
    public LeaderHeartbeatEndpoint leaderHeartbeatEndpoint() {
        return new LeaderHeartbeatEndpoint();
    }

    @Bean
    public ElectionEndpoint electionEndpoint() {
        return new ElectionEndpoint();
    }

    @Bean
    public NodeRegisterEndpoint nodeRegisterEndpoint() {
        return new NodeRegisterEndpoint();
    }

    @Bean
    public BroadcastEndpoint broadcastEndpoint() {
        return new BroadcastEndpoint();
    }

    @Bean
    public CheckLeaderStatusEndpoint checkLeaderStatusEndpoint() {
        return new CheckLeaderStatusEndpoint();
    }

    @Bean
    public NodeService nodeService() {
        return new NodeService(decentralizedDiscoveryProperties);
    }

    @Bean
    public HeartbeatService heartbeatService() {
        return new HeartbeatService();
    }

    @Bean
    public SelfServiceClusterBroadcast selfServiceClusterBroadcast() {
        return new SelfServiceClusterBroadcast();
    }

    @Bean
    public BroadcastListener broadcastListener() {
        return new BroadcastListener();
    }

    @Bean
    public OfflineEventHandler offlineEventHandler() {
        return new OfflineEventHandler();
    }

    @Bean
    public DynamicUrlTarget<DiscoveryFeignClient> discoveryFeignClientHardCodedTarget() {
        return new DynamicUrlTarget<>(DiscoveryFeignClient.class, "node");
    }

    @Bean
    public DiscoveryFeignClient discoveryFeignClient(@Autowired ConfigurableApplicationContext configurableApplicationContext) {
        FeignClientFactoryBean factoryBean = new FeignClientFactoryBean() {
            @Override
            public Object getObject() {
                FeignContext context = configurableApplicationContext != null ? configurableApplicationContext.getBean(FeignContext.class)
                        : configurableApplicationContext.getBean(FeignContext.class);
                Feign.Builder builder = feign(context);
                Client client = getOptional(context, Client.class);
                if (client != null) {
                    if (client instanceof FeignBlockingLoadBalancerClient) {
                        client = ((FeignBlockingLoadBalancerClient) client).getDelegate();
                    }
                    if (client instanceof RetryableFeignBlockingLoadBalancerClient) {
                        client = ((RetryableFeignBlockingLoadBalancerClient) client).getDelegate();
                    }
                    builder.client(client);
                }
                Targeter targeter = get(context, Targeter.class);
                return targeter.target(this, builder, context, discoveryFeignClientHardCodedTarget());
            }

        };

        // 设置必要属性
        factoryBean.setApplicationContext(configurableApplicationContext);
        factoryBean.setType(DiscoveryFeignClient.class);
        factoryBean.setName("node");
        factoryBean.setContextId("DecentralizedDiscovery");

        factoryBean.setDecode404(false);

        // 初始化（触发配置加载）
        factoryBean.afterPropertiesSet();

        // 获取客户端（Feign 动态代理）
        return (DiscoveryFeignClient) factoryBean.getObject();
    }

    @Configuration
    @ConditionalOnClass(name = {"javax.jmdns.JmDNS"})
    public static class JmDnsConfiguration {

        @Bean
        @ConditionalOnClass(name = {"javax.jmdns.JmDNS"})
        public JmDNS jmdns() throws Exception {
            InetAddress address = InetAddress.getLocalHost();
            JmDNS jmDNS = JmDNS.create(address);
            return jmDNS;
        }

        @Bean
        @ConditionalOnClass(name = {"javax.jmdns.JmDNS"})
        @DependsOn("nodeService")
        public JmDnsService jmDnsService() {
            return new JmDnsService();
        }
    }

}
