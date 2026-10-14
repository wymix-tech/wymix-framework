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
import org.wymix.framework.discovery.endpoint.NodeRefreshEndpoint;
import org.wymix.framework.discovery.endpoint.NodeRegisterEndpoint;
import org.wymix.framework.discovery.endpoint.NodesEndpoint;
import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;
import org.wymix.framework.discovery.supports.DiscoveryFeignClient;
import org.wymix.framework.discovery.supports.DynamicUrlTarget;
import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.HeartbeatService;
import org.wymix.framework.discovery.supports.NodeService;
import feign.Client;
import feign.Feign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public NodeRefreshEndpoint nodeRefreshEndpoint() {
        return new NodeRefreshEndpoint();
    }

    @Bean
    public NodesEndpoint nodesEndpoint() {
        return new NodesEndpoint();
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

    /**
     * 构建用于节点间通信的 Feign 客户端。
     *
     * <p><b>为什么手工构造 {@link FeignClientFactoryBean} 而不是用 {@code @FeignClient}</b>：
     * 节点间调用的目标地址是运行期动态决定的（{@link DynamicUrlTarget#setUrl}），
     * 而 {@code @FeignClient} 的 url 在注册时就固定了，无法复用同一个代理去调不同节点。
     *
     * <p><b>Spring Cloud 4.x 适配</b>：
     * <ul>
     *     <li>{@code FeignContext} 已被 {@link FeignClientFactory} 取代（4.1 起移除）；</li>
     *     <li>{@code setDecode404} 已更名为 {@code setDismiss404}（与 feign 13 的 {@code dismiss404} 对齐）。</li>
     * </ul>
     */
    @Bean
    public DiscoveryFeignClient discoveryFeignClient(@Autowired ConfigurableApplicationContext configurableApplicationContext) {
        FeignClientFactoryBean factoryBean = new FeignClientFactoryBean() {
            @Override
            public Object getObject() {
                FeignClientFactory context = configurableApplicationContext.getBean(FeignClientFactory.class);
                Feign.Builder builder = feign(context);
                Client client = getOptional(context, Client.class);
                if (client != null) {
                    // 目标地址由 DynamicUrlTarget 指定，无需负载均衡，剥离外层拿到原始 client
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
        // 手工 new 出来的实例不会触发 BeanFactoryAware 回调，需显式注入
        factoryBean.setBeanFactory(configurableApplicationContext);
        factoryBean.setType(DiscoveryFeignClient.class);
        factoryBean.setName("node");
        factoryBean.setContextId("DecentralizedDiscovery");

        factoryBean.setDismiss404(false);

        // 初始化（触发配置加载）
        factoryBean.afterPropertiesSet();

        // 获取客户端（Feign 动态代理）
        return (DiscoveryFeignClient) factoryBean.getObject();
    }

}
