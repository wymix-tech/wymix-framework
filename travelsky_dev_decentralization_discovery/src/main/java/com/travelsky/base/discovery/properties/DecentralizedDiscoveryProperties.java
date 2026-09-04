/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.properties
 * File Name:DecentralizedDiscoveryProperties
 * Date:2025/9/3 10:53
 */
package com.travelsky.base.discovery.properties;

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
