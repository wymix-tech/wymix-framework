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
import org.wymix.framework.utils.json.JsonUtils;
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

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @WriteOperation
    public void broadcast(@Selector String topic, @Selector String className, String nodes) {
        try {
            Object o = JsonUtils.jsonToEntity(Class.forName(className), nodes);
            applicationEventPublisher.publishEvent(new BroadcastEvent<>(topic, o, this));
        } catch (ClassNotFoundException e) {

        }
    }

}
