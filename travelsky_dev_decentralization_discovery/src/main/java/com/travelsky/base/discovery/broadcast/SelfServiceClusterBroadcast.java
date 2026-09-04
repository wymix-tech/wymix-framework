/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.broadcast
 * File Name:SelfServiceClusterBroadcast
 * Date:2025/9/8 15:41
 */
package com.travelsky.base.discovery.broadcast;

import com.travelsky.base.core.feign.supports.NodeInfo;
import com.travelsky.base.discovery.supports.DiscoveryFeignClient;
import com.travelsky.base.discovery.supports.DynamicUrlTarget;
import com.travelsky.base.discovery.supports.HeartbeatDTO;
import com.travelsky.base.discovery.supports.NodeRegistry;
import com.travelsky.base.tp.supports.ThreadPoolRegistry;
import com.travelsky.base.utils.json.JsonUtils;
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
