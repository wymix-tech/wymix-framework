/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.endpoint
 * File Name:ElectionEndpoint
 * Date:2025/9/3 16:53
 */
package org.wymix.framework.discovery.endpoint;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.supports.ElectionService;
import org.wymix.framework.discovery.supports.NodeRegistry;
import org.wymix.framework.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * ClassName: ElectionEndpoint
 * Description:
 * Date: 2025/9/3 16:53
 *
 * @author wang_yang
 */
@Endpoint(id = "nodeRegister")
public class NodeRegisterEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegisterEndpoint.class);
    @Autowired
    private ElectionService electionService;

    /**
     * 接收对端注册。
     *
     * <p><b>参数名必须是 {@code nodes}</b>：actuator 的 {@code @WriteOperation} 会把 JSON 请求体按
     * <b>参数名</b>绑定（{@code {"nodes":...}} 里的值赋给名为 {@code nodes} 的参数），而不是把整个
     * 请求体反序列化成参数对象。调用方 {@code DiscoveryFeignClient#register} 发送的是
     * {@link HeartbeatDTO}（字段 {@code nodes}），恰好与参数名对应。
     * 一旦把参数改成 {@code HeartbeatDTO} 或改名，绑定就会失败并报
     * {@code MissingParametersException}，节点注册全部 400。
     */
    @WriteOperation
    public NodeInfo register(String nodes) {
        NodeInfo nodeInfo = JsonUtils.jsonToEntity(NodeInfo.class, nodes);
        nodeInfo.setSelf(false);
        nodeInfo.setLeader(false);
        NodeRegistry.signUp(nodeInfo);
        electionService.electLeader(true);
        // 必须返回对象：返回 JSON 字符串会被引擎按 application/json 二次编码成带引号的字符串，
        // 调用方（DiscoveryFeignClient#register 声明为 NodeInfo）将反序列化失败
        return NodeRegistry.self();
    }

}
