/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.broadcast.handler
 * File Name:EventHandler
 * Date:2025/9/9 9:45
 */
package com.travelsky.base.discovery.broadcast.handler;

/**
 * ClassName: EventHandler
 * Description:
 * Date: 2025/9/9 9:45
 *
 * @author wang_yang
 */
public interface EventHandler<T> {

    String topic();

    void handle(T payload);

}
