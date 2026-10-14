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

import org.wymix.framework.discovery.broadcast.event.BroadcastEvent;
import org.wymix.framework.discovery.supports.HeartbeatDTO;
import org.wymix.framework.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationEventPublisher;

/**
 * ClassName: ElectionEndpoint
 * Description: 
 * Date: 2025/9/3 16:53
 *
 * @author wang_yang
 */
@Endpoint(id = "broadcast")
public class BroadcastEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastEndpoint.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired(required = false)
    private java.util.List<org.wymix.framework.discovery.broadcast.handler.EventHandler<?>> handlers = java.util.List.of();
    private final java.util.Map<String, Class<?>> messageTypes = new java.util.HashMap<>();

    @jakarta.annotation.PostConstruct
    public void registerMessageTypes() {
        for (var handler : handlers) {
            Class<?> type = handler.payloadType();
            if (type == Object.class) throw new IllegalArgumentException("Broadcast handlers must declare a concrete payload type");
            Class<?> previous = messageTypes.putIfAbsent(handler.topic(), type);
            if (previous != null && previous != type) throw new IllegalArgumentException("Conflicting broadcast payload types for " + handler.topic());
        }
    }

    /**
     * 接收集群广播。
     *
     * <p>{@code topic} / {@code className} 是 {@code @Selector}（来自路径），
     * {@code nodes} 由请求体按参数名绑定，需与调用方 {@code DiscoveryFeignClient#broadcast}
     * 发送的 {@link HeartbeatDTO} 字段名一致。
     */
    @WriteOperation
    public void broadcast(@Selector String topic, @Selector String className, String nodes) {
        try {
            Class<?> type = messageTypes.get(topic);
            if (type == null || !type.getName().equals(className)) {
                throw new IllegalArgumentException("Unsupported broadcast topic/type");
            }
            Object o = JsonUtils.jsonToEntity(type, nodes);
            if (o == null) throw new IllegalArgumentException("Invalid broadcast payload");
            applicationEventPublisher.publishEvent(new BroadcastEvent<>(topic, o, this));
        } catch (IllegalArgumentException e) {
            // 原实现静默吞掉，广播失败在日志里毫无痕迹，补 warn
            throw e;
        }
    }

}
