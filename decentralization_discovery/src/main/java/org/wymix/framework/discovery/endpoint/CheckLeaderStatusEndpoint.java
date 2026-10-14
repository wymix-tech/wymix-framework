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

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * ClassName: ElectionEndpoint
 * Description: 
 * Date: 2025/9/3 16:53
 *
 * @author wang_yang
 */
@Endpoint(id = "checkLeaderStatus")
public class CheckLeaderStatusEndpoint {

    @ReadOperation
    public boolean checkLeaderStatus() {
        return true;
    }

}
