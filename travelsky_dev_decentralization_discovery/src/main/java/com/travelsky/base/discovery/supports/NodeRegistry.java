/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.supports
 * File Name:NodeRegistry
 * Date:2025/9/3 11:27
 */
package com.travelsky.base.discovery.supports;

import com.travelsky.base.core.feign.supports.DynamicInstanceManager;
import com.travelsky.base.core.feign.supports.NodeInfo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ClassName: NodeRegistry
 * Description:
 * Date: 2025/9/3 11:27
 *
 * @author wang_yang
 */
public final class NodeRegistry {

    private static final ConcurrentMap<String, List<NodeInfo>> NODE_LIST = new ConcurrentHashMap<>();
    private static final AtomicReference<NodeInfo> SELF = new AtomicReference<>();
    private static final AtomicReference<NodeInfo> LEADER = new AtomicReference<>();

    private NodeRegistry() {

    }

    public synchronized static void signUp(NodeInfo nodeInfo) {
        List<NodeInfo> nodes;
        if ((nodes = NODE_LIST.get(nodeInfo.getServiceName())) == null) {
            nodes = NODE_LIST.computeIfAbsent(nodeInfo.getServiceName(), k -> {
                CopyOnWriteArrayList list = new CopyOnWriteArrayList();
                return list;
            });
        }
        Optional<NodeInfo> optional = nodes.stream().filter(node -> node.getHost().equals(node.getHost()) && node.getPort().compareTo(nodeInfo.getPort()) == 0).findFirst();
        if (!optional.isPresent()) {
            if (nodeInfo.isSelf()) {
                SELF.set(nodeInfo);
            }
            nodes.add(nodeInfo);
            DynamicInstanceManager.addServiceInstance(nodeInfo.getServiceName(), nodeInfo.getHost(), nodeInfo.getPort());
        } else {
            optional.get().setLastSeenSuccess(LocalDateTime.now());
            optional.get().setLastSeen(nodeInfo.getLastSeen());
            optional.get().setUpTime(nodeInfo.getUpTime());
        }
    }

    public synchronized static NodeInfo signOut(String serviceName) {
        String[] strs = serviceName.split("#");
        List<NodeInfo> nodes;
        if ((nodes = NODE_LIST.get(strs[0])) != null) {
            Optional<NodeInfo> optional = nodes.stream().filter(nodeInfo -> strs[1].equals(String.format("%s:%d", nodeInfo.getHost(), nodeInfo.getPort()))).findFirst();
            if (optional.isPresent()) {
                NodeInfo nodeInfo = optional.get();
                nodes.remove(nodeInfo);
                return nodeInfo;
            }
        }
        return null;
    }

    public synchronized static long size() {
        return NODE_LIST.values().stream().flatMap(List::stream).count();
    }

    public synchronized static Map<String, List<NodeInfo>> nodes() {
        return new HashMap<>(NODE_LIST);
    }

    public static NodeInfo self() {
        return SELF.get();
    }

    public static NodeInfo leader() {
        return LEADER.get();
    }

    public synchronized static void setLeader(NodeInfo nodeInfo) {
        if (LEADER.get() != null) {
            LEADER.get().setLeader(false);
            LEADER.set(null);
        }
        nodeInfo.setLeader(true);
        LEADER.set(nodeInfo);
    }

}
