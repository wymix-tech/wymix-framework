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
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    /**
     * 端口兜底值。
     *
     * <p>注意：Boot 的默认端口 8080 并不存在于 Environment 中（它由 {@link ServerProperties} 绑定），
     * 因此未显式配置 {@code server.port} 时 {@code @Value} 的默认值就是实际值。原默认值写成 80，
     * 会让节点把自己登记成 80 端口，其它节点按该地址回调必然失败，故兜底值必须是 8080。
     */
    @Value("${server.port:8080}")
    private int port;
    // Boot 3 的属性名是 server.servlet.context-path，server.context-path 仅为旧版兼容
    @Value("${server.servlet.context-path:${server.context-path:/}}")
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
    /**
     * 由 Boot 绑定，可拿到实际生效的端口（覆盖 {@code SERVER_PORT} 等 relaxed binding 形式）。
     * 非 Web 环境下该 Bean 不存在，故为可选注入。
     */
    @Autowired(required = false)
    private ServerProperties serverProperties;

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
        // ServerProperties#getPort() 在未显式配置时为 null（Boot 由底层容器决定默认 8080），必须判空后覆盖
        int actualPort = port;
        if (serverProperties != null && serverProperties.getPort() != null) {
            actualPort = serverProperties.getPort();
        }
        NodeInfo nodeInfo = genNodeInfo(decentralizedDiscoveryProperties.getNamespace(), NetUtils.getLocalhostStr(), actualPort, getSelfNodeName(), LocalDateTime.now(), version, true, false, contextPath);
        NodeRegistry.signUp(nodeInfo);
        syncConfiguredNodes();
        electionService.electLeader(true);
    }

    /**
     * 按配置的服务地址列表登记集群节点。
     *
     * <p>节点来源只有两处，<b>没有自动发现</b>：
     * <ol>
     *     <li>本方法读取的 {@code decentralization.discovery.service.<svc>} 配置地址列表；</li>
     *     <li>其它实例主动调用 {@code /actuator/nodeRegister} 时的双向登记。</li>
     * </ol>
     *
     * <p>已登记的地址（含自身）会跳过，故本方法可重复调用。实例启动时对端尚未就绪无需处理，
     * 对端启动后会主动调 {@code nodeRegister} 完成双向登记；若两端都已就绪却因地址不通而未登记，
     * 可用 {@code POST /actuator/nodeRefresh} 重新同步。
     *
     * @return 同步结果摘要（新登记数、失败数、已知节点数、当前 Leader）
     */
    public String syncConfiguredNodes() {
        int registered = 0;
        int failed = 0;
        NodeInfo self = NodeRegistry.self();
        for (Map.Entry<String, List<String>> entry : decentralizedDiscoveryProperties.getService().entrySet()) {
            for (String raw : entry.getValue()) {
                if (raw == null || raw.trim().isEmpty()) {
                    continue;
                }
                String address = normalize(raw);
                if (address.isEmpty()) {
                    continue;
                }
                String host = hostOf(address);
                // 跳过自身，以及已登记过的地址（避免每次调用都重复注册）
                if (self != null && host.equals(self.getHost())) {
                    continue;
                }
                if (NodeRegistry.find(host, portOf(address)).isPresent()) {
                    continue;
                }
                try {
                    discoveryFeignClientHardCodedTarget.setUrl(address);
                    // 端点返回对端 NodeInfo 的 JSON 对象，feign 直接反序列化，无需再手工转换
                    NodeInfo info = discoveryFeignClient.register(new HeartbeatDTO(JsonUtils.entityToJson(self)));
                    if (info == null) {
                        failed++;
                        continue;
                    }
                    info.setSelf(false);
                    info.setLeader(false);
                    NodeRegistry.signUp(info);
                    registered++;
                } catch (Exception e) {
                    failed++;
                    logger.warn("register node [{}] failed: {}", address, e.getMessage());
                }
            }
        }
        NodeInfo leader = NodeRegistry.leader();
        return String.format("registered=%d, failed=%d, known=%d, leader=%s",
                registered, failed, NodeRegistry.size(), leader == null ? "none" : leader.instanceName());
    }

    /**
     * 归一化服务地址：剥离 {@code http://} 之类的方案前缀与尾部路径。
     *
     * <p>{@code DynamicUrlTarget#url()} 会自行拼接 {@code http://}，若配置里再写方案前缀，
     * 拼出来的就是 {@code http://http://host:port} 这种非法地址，故这里统一先剥离。
     */
    private static String normalize(String address) {
        String normalized = address.trim();
        int schemeIdx = normalized.indexOf("://");
        if (schemeIdx >= 0) {
            normalized = normalized.substring(schemeIdx + 3);
        }
        int pathIdx = normalized.indexOf('/');
        if (pathIdx >= 0) {
            normalized = normalized.substring(0, pathIdx);
        }
        return normalized;
    }

    /**
     * 从 {@code host[:port]} 形式的服务地址中取主机名。
     */
    private static String hostOf(String address) {
        int idx = address.lastIndexOf(':');
        return idx < 0 ? address : address.substring(0, idx);
    }

    /**
     * 从 {@code host[:port]} 形式的服务地址中取端口，缺省 80。
     */
    private static int portOf(String address) {
        int idx = address.lastIndexOf(':');
        if (idx < 0) {
            return 80;
        }
        try {
            return Integer.parseInt(address.substring(idx + 1));
        } catch (NumberFormatException e) {
            return 80;
        }
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
