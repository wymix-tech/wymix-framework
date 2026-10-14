/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:NodeInfo
 * Date:2025/9/3 10:49
 */
package org.wymix.framework.core.feign.supports;

import org.wymix.framework.utils.PriorityUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ClassName: NodeInfo
 * Description:
 * Date: 2025/9/3 10:49
 *
 * @author wang_yang
 */
public class NodeInfo implements Serializable {

    private String namespace;

    private String host;

    private Integer port = 80;

    private String contextPath;

    private String serviceName;

    private String version;

    private HealthStatus status;

    private LocalDateTime upTime;

    private LocalDateTime downTime;

    private LocalDateTime lastSeen;

    private LocalDateTime lastSeenSuccess;

    private boolean leader;

    private boolean self;

    private int brokenTimes = 0;

    private long lossTime = 0;

    private int term = 0;

    public double priorityForUp() {
        return priority(true);
    }

    public double priorityForDown() {
        return priority(false);
    }

    public double priority(boolean up) {
        return PriorityUtils.calculateStabilityScore(leader, upTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), lossTime, brokenTimes, up ? 0 : 0);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public void setStatus(HealthStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpTime() {
        return upTime;
    }

    public void setUpTime(LocalDateTime upTime) {
        this.upTime = upTime;
    }

    public LocalDateTime getDownTime() {
        return downTime;
    }

    public void setDownTime(LocalDateTime downTime) {
        this.downTime = downTime;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }

    public int getBrokenTimes() {
        return brokenTimes;
    }

    public void setBrokenTimes(int brokenTimes) {
        this.brokenTimes = brokenTimes;
    }

    public long getLossTime() {
        return lossTime;
    }

    public void setLossTime(long lossTime) {
        this.lossTime = lossTime;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public LocalDateTime getLastSeenSuccess() {
        return lastSeenSuccess;
    }

    public void setLastSeenSuccess(LocalDateTime lastSeenSuccess) {
        this.lastSeenSuccess = lastSeenSuccess;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String instanceName() {
        return String.format("%s#%s:%d", serviceName, host, port);
    }


    public String host() {
        return String.format("%s:%d", host, port);
    }

    public String url() {
        return String.format("%s:%d%s", host, port, contextPath);
    }
}
