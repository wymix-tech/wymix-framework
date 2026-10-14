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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: ElectionService
 * Description: Leader 选举
 * Date: 2025/9/4 10:28
 *
 * @author wang_yang
 */
public class ElectionService {

    private static final Logger logger = LoggerFactory.getLogger(ElectionService.class);
    @Value("${spring.application.name:default}")
    private String appName;


    public ElectionService() {
    }

    /**
     * 选举集群 Leader。
     *
     * <p>去中心化发现没有注册中心，每个实例都是各自计算后得出 Leader，因此正确性要求是
     * <b>各实例独立计算却必须得到同一个结果</b>，否则就会出现多 Leader（脑裂）。为此做了四点：
     * <ol>
     *     <li><b>确定性决胜</b>：得分相同时按 {@code instanceName}（{@code serviceName#host:port}）
     *     字典序取最小者，与节点在列表中的先后无关。原实现 {@code sorted(...).max(...)} 里的
     *     {@code sorted} 对 {@code max} 的结果没有任何影响（白做一次全量排序），
     *     且平局时 {@code max} 返回「先遇到者」，而各实例注册顺序不同，直接导致选出不同 Leader。</li>
     *     <li><b>得分可比</b>：得分经 {@link org.wymix.framework.utils.PriorityUtils} 量化到 4 位小数，
     *     抹平各实例本地时钟漂移带来的毫秒级差异，避免出现「同一节点在不同实例上得分不同」。</li>
     *     <li><b>Leader 粘性</b>：常规选举（{@code up=true}，如新节点注册）时，现任 Leader 只要仍在
     *     候选列表中就直接连任，不会因为集群里多一个节点就换 Leader，也把各实例产生分歧的机会降到最低。</li>
     *     <li><b>故障重选</b>：{@code up=false}（Leader 心跳失联、显式触发重选）时不做粘性判断，
     *     并剔除已判定为 {@link HealthStatus#DOWN} 的候选，重新按得分与字典序选出。</li>
     * </ol>
     *
     * <p><b>已知限制</b>：这是无共识协议的「各自计算」式选举，没有任期（term）与投票阶段。
     * 网络分区恢复的瞬间仍可能短暂出现双 Leader；业务若强依赖唯一 Leader，请在上层补租约校验。
     *
     * @param up true=节点上线/加入触发的常规选举；false=Leader 失效后的重新选举
     * @return 选出的 Leader；当前服务无任何节点时返回 null
     */
    public synchronized NodeInfo electLeader(boolean up) {
        Map<String, List<NodeInfo>> allNodes = NodeRegistry.nodes();
        List<NodeInfo> nodeInfos = allNodes.getOrDefault(this.appName, Collections.emptyList());
        if (nodeInfos.isEmpty()) {
            logger.warn("No node registered for service [{}], skip leader election", this.appName);
            return null;
        }

        // 只有一个节点时无需比较，直接由它担任 Leader
        if (nodeInfos.size() == 1) {
            return appoint(nodeInfos.get(0));
        }

        List<NodeInfo> candidates = alive(nodeInfos);
        if (candidates.isEmpty()) {
            // 所有节点都被判定为 DOWN：宁可产出一个 Leader，也不让集群处于无主状态
            candidates = nodeInfos;
        }

        if (up) {
            NodeInfo currentLeader = NodeRegistry.leader();
            if (currentLeader != null && candidates.contains(currentLeader)) {
                logger.debug("Service Cluster Leader stays: {}", currentLeader.instanceName());
                return appoint(currentLeader);
            }
        }

        return appoint(candidates.stream().max(winnerComparator(candidates.size())).orElse(null));
    }

    /**
     * 剔除已判定为 DOWN 的节点。
     */
    private static List<NodeInfo> alive(List<NodeInfo> nodeInfos) {
        return nodeInfos.stream()
                .filter(nodeInfo -> nodeInfo.getStatus() != HealthStatus.DOWN)
                .collect(Collectors.toList());
    }

    /**
     * 胜出者比较器：得分升序 + 实例名字典序降序。
     *
     * <p>配合 {@code max} 使用，等价于「得分最高者胜；得分相同则实例名字典序最小者胜」。
     * 字典序这一级是必需的决胜条件：只比得分时，得分相同的候选在各实例上的胜负取决于列表顺序，
     * 而各实例的列表顺序由注册先后决定，并不一致。
     */
    private static Comparator<NodeInfo> winnerComparator(long clusterSize) {
        return Comparator.<NodeInfo>comparingDouble(nodeInfo -> nodeInfo.priority(clusterSize))
                .thenComparing(Comparator.comparing(NodeInfo::instanceName).reversed());
    }

    /**
     * 登记选举结果。
     */
    private static NodeInfo appoint(NodeInfo elected) {
        if (elected == null) {
            return null;
        }
        NodeInfo previous = NodeRegistry.leader();
        NodeRegistry.setLeader(elected);
        if (previous != elected) {
            logger.info("Service Cluster Leader is: {}", elected.instanceName());
        } else {
            logger.debug("Service Cluster Leader is: {}", elected.instanceName());
        }
        return elected;
    }

}
