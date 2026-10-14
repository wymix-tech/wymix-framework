package org.wymix.framework.dynamic.datasource.support;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.sql.DataSource;


public class DynamicDataSourceBuilder {

    private final ConfigurableEnvironment environment;

    public DynamicDataSourceBuilder(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public DataSource build(String prefix) {
        String type = environment.getProperty(prefix + ".type");
        if (type == null || type.isEmpty()) {
            type = "com.zaxxer.hikari.HikariDataSource"; // 默认 Hikari
        }

        try {
            Class<DataSource> dataSourceClass = (Class<DataSource>) Class.forName(type);
            DataSource dataSource = dataSourceClass.getDeclaredConstructor().newInstance();

            // 使用 Spring Boot 的 Binder 绑定属性到 DataSource 对象
            Bindable<DataSource> bindable = (Bindable<DataSource>) Bindable.of(dataSourceClass).withExistingValue(dataSource);
            Binder.get(environment).bind(prefix, bindable);
            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException("创建数据源失败: " + prefix, e);
        }
    }
}
