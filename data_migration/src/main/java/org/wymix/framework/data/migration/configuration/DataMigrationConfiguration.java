package org.wymix.framework.data.migration.configuration;

import org.wymix.framework.data.migration.listener.DataMigrationEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 数据迁移自动装配。
 *
 * <p>本模块只监听 {@code DataChangeLogEvent}，真正的变更日志由 jpa / mybatis 两个实现模块提供；
 * 业务方在同一应用里只会引入其中一个，因此这里必须<b>按 classpath 条件</b>导入对应的
 * {@code DataChangeLogConfiguration}，不能静态 {@code @Import} 某一个 ORM 的实现
 * （否则纯 MyBatis 场景会因找不到 JPA 的变更日志配置类而启动失败）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.data-migration", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class DataMigrationConfiguration {

    @Bean
    public DataMigrationEventListener dataMigrationEventListener() {
        return new DataMigrationEventListener();
    }

    /**
     * JPA 场景：classpath 存在 JPA 实现时导入其数据变更日志配置。
     * 使用 {@code name} 形式避免条件求值时提前加载不存在的类。
     */
    @Configuration
    @ConditionalOnClass(name = "org.wymix.framework.repository.jpa.datachangelog.configuration.DataChangeLogConfiguration")
    @Import(org.wymix.framework.repository.jpa.datachangelog.configuration.DataChangeLogConfiguration.class)
    static class JpaDataChangeLogConfiguration {
    }

    /**
     * MyBatis 场景：classpath 存在 MyBatis 实现时导入其数据变更日志配置。
     */
    @Configuration
    @ConditionalOnClass(name = "org.wymix.framework.repository.mybatis.datachangelog.configuration.DataChangeLogConfiguration")
    @Import(org.wymix.framework.repository.mybatis.datachangelog.configuration.DataChangeLogConfiguration.class)
    static class MyBatisDataChangeLogConfiguration {
    }

}
