/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.broadcast
 * File Name:SelfServiceClusterBroadcast
 * Date:2025/9/8 15:41
 */
package org.wymix.framework.discovery.broadcast;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.supports.DiscoveryFeignClient;
import org.wymix.framework.discovery.supports.DynamicUrlTarget;
import org.wymix.framework.discovery.supports.HeartbeatDTO;
import org.wymix.framework.discovery.supports.NodeRegistry;
import org.wymix.framework.tp.supports.ThreadPoolRegistry;
import org.wymix.framework.utils.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * ClassName: SelfServiceClusterBroadcast
 * Description:
 * Date: 2025/9/8 15:41
 *
 * @author wang_yang
 */
public class SelfServiceClusterBroadcast {

    private final ExecutorService executorService;
    @Autowired
    private DiscoveryFeignClient discoveryFeignClient;
    @Autowired
    private DynamicUrlTarget<DiscoveryFeignClient> discoveryFeignClientHardCodedTarget;

    public SelfServiceClusterBroadcast() {
        executorService = ThreadPoolRegistry.newFixedThreadPool("self-service-cluster", 8, 100);
    }

    public CompletableFuture<Void> sendAsync(String topic, Object object, boolean containSelf) {
        NodeInfo self = NodeRegistry.self();
        List<NodeInfo> nodeInfos = NodeRegistry.nodes().get(self.getServiceName());
        CompletableFuture<Void>[] futures = new CompletableFuture[containSelf ? nodeInfos.size() : (nodeInfos.size() - 1)];
        int index = 0;
        for (NodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.isSelf() && !containSelf) {
                continue;
            }
            futures[index++] = CompletableFuture.runAsync(() -> {
                try {
                    discoveryFeignClientHardCodedTarget.setUrl(nodeInfo.url());
                    discoveryFeignClient.broadcast(topic, object.getClass().getName(), new HeartbeatDTO(JsonUtils.entityToJson(object)));
                } catch (Exception e) {
                    // DO NOTHING
                }
            }, executorService);
        }
        return CompletableFuture.allOf(futures);
    }

    public void sendSync(String topic, Object object, boolean containSelf) throws ExecutionException, InterruptedException {
        sendAsync(topic, object, containSelf).get();
    }

}
