/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.configuration
 * File Name:DecentraDiscoveryConfiguration
 * Date:2025/9/3 10:44
 */
package com.travelsky.base.discovery.configuration;

import com.travelsky.base.core.configuration.CustomFeignConfiguration;
import com.travelsky.base.discovery.broadcast.SelfServiceClusterBroadcast;
import com.travelsky.base.discovery.broadcast.handler.OfflineEventHandler;
import com.travelsky.base.discovery.broadcast.listener.BroadcastListener;
import com.travelsky.base.discovery.endpoint.BroadcastEndpoint;
import com.travelsky.base.discovery.endpoint.CheckLeaderStatusEndpoint;
import com.travelsky.base.discovery.endpoint.ElectionEndpoint;
import com.travelsky.base.discovery.endpoint.LeaderHeartbeatEndpoint;
import com.travelsky.base.discovery.endpoint.NodeRegisterEndpoint;
import com.travelsky.base.discovery.properties.DecentralizedDiscoveryProperties;
import com.travelsky.base.discovery.supports.DiscoveryFeignClient;
import com.travelsky.base.discovery.supports.DynamicUrlTarget;
import com.travelsky.base.discovery.supports.ElectionService;
import com.travelsky.base.discovery.supports.HeartbeatService;
import com.travelsky.base.discovery.supports.JmDnsService;
import com.travelsky.base.discovery.supports.NodeService;
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
