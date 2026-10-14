/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.broadcast.handler
 * File Name:EventHandler
 * Date:2025/9/9 9:45
 */
package org.wymix.framework.discovery.broadcast.handler;

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
