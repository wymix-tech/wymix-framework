/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.broadcast.event
 * File Name:BroadcastEvent
 * Date:2025/9/8 15:55
 */
package org.wymix.framework.discovery.broadcast.event;

import org.springframework.context.ApplicationEvent;

/**
 * ClassName: BroadcastEvent
 * Description:
 * Date: 2025/9/8 15:55
 *
 * @author wang_yang
 */
public class BroadcastEvent<T> extends ApplicationEvent {
    private final T payload;
    private final String topic;

    public BroadcastEvent(String topic, T payload, Object source) {
        super(source);
        this.payload = payload;
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }


    public T payload() {
        return payload;
    }
}
