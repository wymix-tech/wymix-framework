/**
 * Copyright (c) 2025,TravelSky.
 * All Rights Reserved.
 * TravelSky CONFIDENTIAL
 * <p>
 * Project Name:travelsky_dev_parent
 * Package Name:com.travelsky.base.discovery.supports
 * File Name:PriorityUtils
 * Date:2025/9/3 14:35
 */
package com.travelsky.base.utils;

/**
 * ClassName: PriorityUtils
 * Description:
 * Date: 2025/9/3 14:35
 *
 * @author wang_yang
 */
public class PriorityUtils {


    // 健康度权重（失联次数影响）
    private static final double HEALTH_WEIGHT = 0.6;
    // 持续性权重（运行时长和启动时间影响）
    private static final double DURATION_WEIGHT = 0.4;

    // 失联惩罚因子，控制失联对得分的衰减程度，值越大影响越缓和
    private static final double LOSS_PENALTY_FACTOR = 8.0;

    private static final double IMPORTANCE_WEIGHT = 0.3;

    private static final double LEADER_BONUS_WEIGHT = 0.05;

    /**
     * 计算稳定性得分（包含注册表服务器数量影响）
     *
     * @param firstStartTime      项目首次启动时间戳（毫秒）
     * @param lossTime            累计运行时间（毫秒）
     * @param heartbeatLossCount  心跳失联次数
     * @param registryServerCount 当前持有的服务器注册表数量（越多越重要）
     * @return 稳定性得分（0~100），保留4位小数
     */
    public static double calculateStabilityScore(
            boolean leader,
            long firstStartTime,
            long lossTime,
            int heartbeatLossCount,
            long registryServerCount) {

        // 1. 计算基础稳定性（原逻辑）
        double baseStability = calculateBaseStability(firstStartTime, lossTime, heartbeatLossCount);

        // 2. 计算重要性得分（基于注册表数量）
        double importanceScore = calculateImportanceScore(registryServerCount);

        // 3. 加权综合得分
        double finalScore = (1 - IMPORTANCE_WEIGHT) * baseStability + IMPORTANCE_WEIGHT * importanceScore;

//        finalScore = finalScore + (leader ? LEADER_BONUS_WEIGHT * 100 : 0);

        // 限制在 [0, 100] 范围内
        finalScore = Math.max(0, Math.min(100, finalScore));

        // 保留 4 位小数
        return finalScore;
    }

    /**
     * 计算项目稳定性得分
     *
     * @param firstStartTime     项目首次启动时间戳（毫秒）
     * @param lossTime           累计失踪时间（毫秒）
     * @param heartbeatLossCount 心跳失联次数
     * @return 稳定性得分 [0.0, 100.0]
     */
    public static double calculateBaseStability(
            long firstStartTime,
            long lossTime,
            int heartbeatLossCount) {

        // 1. 健康度得分：基于失联次数，指数衰减
        double healthScore = 100.0 / (1 + (double) heartbeatLossCount / LOSS_PENALTY_FACTOR);

        // 防止异常值
        healthScore = Math.max(0, Math.min(100, healthScore));

        // 2. 持续性得分：基于启动时间和运行时间
        long upDuration = System.currentTimeMillis() - firstStartTime;
        if (upDuration <= 0) upDuration = 1; // 防除零

        // 运行时间占比：实际运行时间 / 应存在时间
        double runtimeRatio = (double) (upDuration - lossTime) / upDuration;
        double durationScore = Math.min(runtimeRatio * 100, 100);

        // 3. 加权综合得分
        double stability = upDuration * 0.0000000000075 + HEALTH_WEIGHT * healthScore + DURATION_WEIGHT * durationScore;
        return stability;
    }

    private static double calculateImportanceScore(long nodeCount) {
        if (nodeCount <= 0) return 0;

        // 使用对数增长函数：score = 100 * log10(1 + count) / log10(101)
        // 当 count=100 时，score ≈ 100 * 2/2 = 100
        double maxExpected = 100; // 假设最大常见数量
        double score = 100.0 * Math.log10(1 + nodeCount) / Math.log10(1 + maxExpected);
        return Math.min(score, 100);
    }

}
