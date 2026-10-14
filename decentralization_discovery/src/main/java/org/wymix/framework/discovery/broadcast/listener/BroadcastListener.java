/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.broadcast.listener
 * File Name:BroadcastListener
 * Date:2025/9/9 9:42
 */
package org.wymix.framework.discovery.broadcast.listener;

import org.wymix.framework.discovery.broadcast.event.BroadcastEvent;
import org.wymix.framework.discovery.broadcast.handler.EventHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ClassName: BroadcastListener
 * Description:
 * Date: 2025/9/9 9:42
 *
 * @author wang_yang
 */
public class BroadcastListener implements ApplicationListener<BroadcastEvent<Object>>, ApplicationContextAware {

    private ConcurrentMap<String, List<EventHandler<Object>>> handlers = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @PostConstruct
    public synchronized void init() {
        Map<String, EventHandler> beans = applicationContext.getBeansOfType(EventHandler.class);
        if (beans != null && !beans.isEmpty()) {
            beans.values().forEach(b -> {
                if (handlers.get(b.topic()) == null) {
                    handlers.put(b.topic(), new ArrayList<>());
                }
                handlers.get(b.topic()).add(b);
            });
        }
    }

    @Override
    public void onApplicationEvent(BroadcastEvent<Object> event) {
        String topic = event.topic();
        Object payload = event.payload();
        handlers.getOrDefault(topic, java.util.List.of()).forEach(h -> h.handle(payload));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
