package com.swcares.sample.container;

import org.wymix.framework.test.containers.db.AbstractMySqlContainer;

/**
 * test 示例：基于 Testcontainers 的 MySQL 容器基类。
 *
 * <p>继承 {@link AbstractMySqlContainer} 即获得一个随测试启停的 MySQL 实例，
 * 容器启停由 {@code @Testcontainers} 管理，连接信息通过 {@code @DynamicPropertySource} 注入 Spring 环境。
 * 镜像、账号、库名可用 {@code unittest.properties} 覆盖：
 * {@code db.mysql.image} / {@code db.username} / {@code db.password} / {@code db.database}，
 * 初始化脚本默认读取 {@code schema/*.sql}。
 */
public abstract class SampleMySqlContainer extends AbstractMySqlContainer {
}
