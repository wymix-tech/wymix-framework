package com.travelsky.base.repository.datachangelog.configuration;

import com.travelsky.base.repository.datachangelog.listener.DataChangeLogEventListener;
import com.travelsky.base.repository.datachangelog.plugin.DataChangeLogPlugin;
import com.travelsky.base.repository.datachangelog.plugin.DataChangeLogTableRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

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
