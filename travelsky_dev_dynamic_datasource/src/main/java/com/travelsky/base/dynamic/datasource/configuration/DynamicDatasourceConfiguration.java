package com.travelsky.base.dynamic.datasource.configuration;

import com.travelsky.base.dynamic.datasource.annotations.DynamicDatasource;
import com.travelsky.base.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.travelsky.base.dynamic.datasource.aop.DynamicDataSourceAnnotationInterceptor;
import com.travelsky.base.dynamic.datasource.properties.DynamicDatasourceProperties;
import com.travelsky.base.dynamic.datasource.support.DynamicDataSource;
import com.travelsky.base.dynamic.datasource.support.DynamicDataSourceBuilder;
import com.travelsky.base.dynamic.datasource.support.DynamicDataSourceContextHolder;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableConfigurationProperties(DynamicDatasourceProperties.class)
@ConditionalOnProperty(prefix = "dynamic.datasource", name = "enabled", havingValue = "true")
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class DynamicDatasourceConfiguration {

    @Bean
    public Advisor dynamicDatasourceAnnotationAdvisor() {
        DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor();
        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor, DynamicDatasource.class);
        advisor.setOrder(-10);
        return advisor;
    }

    @Bean
    public DynamicDataSourceBuilder dynamicDataSourceBuilder(@Autowired ConfigurableEnvironment environment) {
        return new DynamicDataSourceBuilder(environment);
    }

    @Bean
    public DataSource dataSource(@Autowired DynamicDatasourceProperties dynamicDatasourceProperties, @Autowired DynamicDataSourceBuilder dynamicDataSourceBuilder) {
        AbstractRoutingDataSource dynamicDataSource = new DynamicDataSource();

        String prefix;
        DataSource dataSource;
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (Map.Entry<String, DynamicDatasourceProperties.DataSourceConfig> entry : dynamicDatasourceProperties.getItems().entrySet()) {
            prefix = "dynamic.datasource.items." + entry.getKey();
            dataSource = dynamicDataSourceBuilder.build(prefix);
            if (dynamicDatasourceProperties.getPrimary().equals(entry.getKey())) {
                dynamicDataSource.setDefaultTargetDataSource(dataSource);
            }
            targetDataSources.put(entry.getKey(), dataSource);
            DynamicDataSourceContextHolder.addDataSource(entry.getKey());
        }

        dynamicDataSource.setTargetDataSources(targetDataSources);
        return dynamicDataSource;
    }

}
