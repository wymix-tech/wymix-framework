/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.endpoint
 * File Name:ElectionEndpoint
 * Date:2025/9/3 16:53
 */
package com.travelsky.base.discovery.endpoint;

import com.travelsky.base.discovery.broadcast.event.BroadcastEvent;
import com.travelsky.base.utils.json.JsonUtils;
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
