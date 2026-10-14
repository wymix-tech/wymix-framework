/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:ElectionService
 * Date:2025/9/4 10:28
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.HealthStatus;
import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.broadcast.SelfServiceClusterBroadcast;
import org.wymix.framework.discovery.enums.SystemTopic;
import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;
import org.wymix.framework.utils.NetUtils;
import org.wymix.framework.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * ClassName: ElectionService
 * Description:
 * Date: 2025/9/4 10:28
 *
 * @author wang_yang
 */
public class NodeService {

    private static final Logger logger = LoggerFactory.getLogger(NodeService.class);
    private DecentralizedDiscoveryProperties decentralizedDiscoveryProperties;
    @Value("${server.port:80}")
    private int port;
    @Value("${server.context-path:/}")
    private String contextPath;
    @Value("${spring.application.name:default}")
    private String appName;
    @Value("${spring.application.version:default}")
    private String version;
    @Autowired
    private DiscoveryFeignClient discoveryFeignClient;
    @Autowired
    private DynamicUrlTarget<DiscoveryFeignClient> discoveryFeignClientHardCodedTarget;
    @Autowired
    private SelfServiceClusterBroadcast selfServiceClusterBroadcast;
    @Autowired
    private ElectionService electionService;

    public NodeService(DecentralizedDiscoveryProperties decentralizedDiscoveryProperties) {
        this.decentralizedDiscoveryProperties = decentralizedDiscoveryProperties;
    }

    public static NodeInfo genNodeInfo(String namespace, String hostAddress,
                                       int port, String serviceName,
                                       LocalDateTime upTime, String version,
                                       boolean self, boolean leader, String contextPath) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setNamespace(namespace);
        nodeInfo.setHost(hostAddress);
        nodeInfo.setPort(port);
        nodeInfo.setServiceName(serviceName.replace(String.format("#%s:%d", nodeInfo.getHost(), nodeInfo.getPort()), ""));
        nodeInfo.setStatus(HealthStatus.UP);
        nodeInfo.setUpTime(upTime);
        nodeInfo.setVersion(version);
        nodeInfo.setLastSeen(LocalDateTime.now());
        nodeInfo.setLastSeenSuccess(LocalDateTime.now());
        nodeInfo.setSelf(self);
        nodeInfo.setLeader(leader);
        nodeInfo.setContextPath(contextPath);
        if ("/".equals(nodeInfo.getContextPath())) {
            nodeInfo.setContextPath("");
        }
        return nodeInfo;
    }

    @PostConstruct
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public void init() {
        NodeInfo nodeInfo = genNodeInfo(decentralizedDiscoveryProperties.getNamespace(), NetUtils.getLocalhostStr(), port, getSelfNodeName(), LocalDateTime.now(), version, true, false, contextPath);
        NodeRegistry.signUp(nodeInfo);
        Map<String, List<String>> service = decentralizedDiscoveryProperties.getService();
        Set<Map.Entry<String, List<String>>> entries = service.entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            entry.getValue().forEach(host -> {
                try {
                    if (host.contains(NodeRegistry.self().host())) {
                        return;
                    }
                    discoveryFeignClientHardCodedTarget.setUrl(host);
                    String json = discoveryFeignClient.register(new HeartbeatDTO(JsonUtils.entityToJson(nodeInfo)));
                    NodeInfo info = JsonUtils.jsonToEntity(NodeInfo.class, json);
                    info.setSelf(false);
                    info.setLeader(false);
                    NodeRegistry.signUp(info);
                } catch (Exception e) {
                    // DO NOTHING
                    logger.warn("find other Node error {}", e.getMessage());
                }
            });
        }
        electionService.electLeader(true);
    }

    public String getSelfNodeName() {
        return String.format("%s#%s:%d", this.appName, NetUtils.getLocalhostStr(), this.port);
    }

    @PreDestroy
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public void destroy() {
        try {
            selfServiceClusterBroadcast.sendSync(SystemTopic.OFFLINE.topic(), NodeRegistry.self().instanceName(), false);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
