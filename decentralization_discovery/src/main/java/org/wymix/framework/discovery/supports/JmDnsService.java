/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:JmDnsService
 * Date:2025/9/9 14:38
 */
package org.wymix.framework.discovery.supports;

import org.wymix.framework.core.feign.supports.NodeInfo;
import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: JmDnsService
 * Description: 
 * Date: 2025/9/9 14:38
 *
 * @author wang_yang
 */
public class JmDnsService {

    @Autowired
    private JmDNS jmdns;

    @Autowired
    private DecentralizedDiscoveryProperties decentralizedDiscoveryProperties;

    @Autowired
    private ElectionService electionService;

    @PostConstruct
    @Order(value = Ordered.LOWEST_PRECEDENCE)
    public void createJmDNS() {
        jmdns.addServiceListener(decentralizedDiscoveryProperties.type(), serviceListener());
        registerSelfServiceInfo();
    }

    private void registerSelfServiceInfo() {
        Map<String, Object> props = new HashMap<>();
        props.put("version", NodeRegistry.self().getVersion());
        props.put("contextPath", NodeRegistry.self().getContextPath());
        LocalDateTime now = NodeRegistry.self().getUpTime();
        long epochMilli = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        props.put("upTime", String.valueOf(epochMilli));
        ServiceInfo serviceInfo = ServiceInfo.create(decentralizedDiscoveryProperties.type(), NodeRegistry.self().instanceName(), NodeRegistry.self().getPort(), 0, 0, props);
        try {
            jmdns.registerService(serviceInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public ServiceListener serviceListener() {
        return new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                NodeInfo nodeInfo = NodeRegistry.signOut(event.getInfo().getName());
                if (nodeInfo != null && nodeInfo.isLeader()) {
                    electionService.electLeader(false);
                }
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                if (!event.getInfo().getPropertyNames().hasMoreElements()) {
                    return;
                }
                String name = event.getInfo().getName();
                if (NodeRegistry.self().instanceName().equals(name)) {
                    return;
                }
                String hostAddress = event.getInfo().getHostAddresses()[0];
                NodeInfo nodeInfo = NodeService.genNodeInfo(decentralizedDiscoveryProperties.getNamespace(),
                        hostAddress,
                        event.getInfo().getPort(),
                        name.replace(String.format("#%s:%d", hostAddress, event.getInfo().getPort()), ""),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.valueOf(event.getInfo().getPropertyString("upTime"))), ZoneId.systemDefault()),
                        event.getInfo().getPropertyString("version"),
                        false,
                        false,
                        event.getInfo().getPropertyString("contextPath"));
                NodeRegistry.signUp(nodeInfo);
                electionService.electLeader(true);
            }
        };
    }

    @PreDestroy
    @Order(value = Ordered.LOWEST_PRECEDENCE)
    public void destroy() {
        try {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
            }
        } catch (Exception e) {

        }
    }
}
