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
import com.fasterxml.jackson.annotation.JsonFormat;

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

    /**
     * 时间字段显式声明格式。
     *
     * <p>节点信息要在实例之间经 Feign 传输：序列化侧用的是框架 JsonUtils 的
     * {@code yyyy-MM-dd HH:mm:ss}，而反序列化侧（Feign 的 Jackson 解码器）默认按 ISO
     * （{@code yyyy-MM-ddTHH:mm:ss}）解析，带空格的格式会直接失败，导致整个节点对象反序列化为 null。
     * 显式声明 pattern 可让两侧一致。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime upTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime downTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeen;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeenSuccess;

    private boolean leader;

    private boolean self;

    private int brokenTimes = 0;

    private long lossTime = 0;

    private int term = 0;

    /**
     * 计算本节点的稳定性得分，作为 Leader 选举的排序依据。
     *
     * <p>{@code clusterSize} 透传给 {@link PriorityUtils#calculateStabilityScore} 的
     * registryServerCount 形参。原实现该形参写死为 {@code up ? 0 : 0}（恒为 0），
     * 导致 priorityForUp / priorityForDown 两条分支得分完全相同，调用方区分 up/down 形同虚设。
     * 现改为由调用方传入真实的候选规模；up/down 的行为差异改由 {@code ElectionService}
     * 通过「是否保持现任 Leader」「是否剔除 DOWN 节点」体现，不再混在打分里。
     *
     * @param clusterSize 本次选举的候选规模（各实例传同一值，保证得分在各实例间可比）
     * @return 稳定性得分（0~100，保留 4 位小数）
     */
    public double priority(long clusterSize) {
        return PriorityUtils.calculateStabilityScore(leader, upTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), lossTime, brokenTimes, clusterSize);
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
