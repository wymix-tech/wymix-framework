package com.travelsky.base.repository.mybatis.datachangelog.configuration;

import com.travelsky.base.repository.datachangelog.listener.DataChangeLogEventListener;
import com.travelsky.base.repository.datachangelog.plugin.DataChangeLogPlugin;
import com.travelsky.base.repository.datachangelog.plugin.DataChangeLogTableRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@ConditionalOnProperty(prefix = "data-changelog", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataChangeLogConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public DataChangeLogTableRegistry dataChangeLogTableRegistry() {
        DataChangeLogTableRegistry dataChangeLogTableRegistry = new DataChangeLogTableRegistry();
        dataChangeLogTableRegistry.setBasePackages(environment.getProperty("data.changelog.basePackages", "").split(","));
        return dataChangeLogTableRegistry;
    }

    @Bean
    @ConditionalOnMissingBean(value = DataChangeLogEventListener.class)
    public DataChangeLogEventListener defaultDataChangeLogEventListener() {
        return new DataChangeLogEventListener();
    }

    @Bean
    public DataChangeLogPlugin dataChangeLogPlugin() {
        return new DataChangeLogPlugin();
    }

}
