/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:NodeRegistry
 * Date:2025/9/3 11:27
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.DynamicInstanceManager;
import org.wymix.framework.core.feign.supports.NodeInfo;

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
        // 按 host + port 判重。原条件写成 node.getHost().equals(node.getHost())（恒真），
        // 导致同端口不同主机的节点被误判为已存在而不再登记，选举的候选集会少掉节点
        Optional<NodeInfo> optional = nodes.stream()
                .filter(node -> node.getHost().equals(nodeInfo.getHost()) && node.getPort().compareTo(nodeInfo.getPort()) == 0)
                .findFirst();
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

    /**
     * 按 host + port 查找已登记的节点，用于登记前判重。
     *
     * @param host 主机地址
     * @param port 端口
     * @return 命中的节点；未登记时为空
     */
    public synchronized static Optional<NodeInfo> find(String host, int port) {
        if (host == null) {
            return Optional.empty();
        }
        return NODE_LIST.values().stream().flatMap(List::stream)
                .filter(nodeInfo -> host.equals(nodeInfo.getHost()) && nodeInfo.getPort() != null
                        && nodeInfo.getPort().compareTo(port) == 0)
                .findFirst();
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

    /**
     * 设置 Leader。
     *
     * <p>幂等：重新选出的仍是现任 Leader 时直接返回。原实现会先 {@code setLeader(false)} 再
     * {@code setLeader(true)}，中间这一帧里 {@code leader()} 指向的节点 {@code isLeader()} 为 false，
     * 并发读取方会看到「Leader 存在但自称不是 Leader」的矛盾状态。
     *
     * @param nodeInfo 新 Leader，为 null 时忽略
     */
    public synchronized static void setLeader(NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        NodeInfo current = LEADER.get();
        if (current == nodeInfo) {
            // 连任：只补一次标记，不产生 false 中间态
            nodeInfo.setLeader(true);
            return;
        }
        if (current != null) {
            current.setLeader(false);
        }
        nodeInfo.setLeader(true);
        LEADER.set(nodeInfo);
    }

    /**
     * 清空节点表、自身节点与 Leader 引用。
     *
     * <p>仅用于单元测试重置与运维重置；运行期调用会丢失整个集群视图，需重新走注册流程。
     */
    public synchronized static void clear() {
        NODE_LIST.clear();
        SELF.set(null);
        LEADER.set(null);
    }

}
