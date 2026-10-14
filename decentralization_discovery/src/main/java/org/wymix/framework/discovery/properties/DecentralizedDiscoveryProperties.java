/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.properties
 * File Name:DecentralizedDiscoveryProperties
 * Date:2025/9/3 10:53
 */
package org.wymix.framework.discovery.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: DecentralizedDiscoveryProperties
 * Description:
 * Date: 2025/9/3 10:53
 *
 * @author wang_yang
 */
@ConfigurationProperties(prefix = "decentralization.discovery")
public class DecentralizedDiscoveryProperties {

    private String namespace;

    private Map<String, List<String>> service = new HashMap<>();

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, List<String>> getService() {
        return service;
    }

    public void setService(Map<String, List<String>> service) {
        this.service = service;
    }

    public String type() {
        return String.format("_%s._tcp.local.", namespace);
    }

}
