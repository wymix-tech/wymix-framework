package org.wymix.framework.repository.mybatis.datachangelog.configuration;

import org.wymix.framework.repository.datachangelog.listener.DataChangeLogEventListener;
import org.wymix.framework.repository.datachangelog.plugin.DataChangeLogPlugin;
import org.wymix.framework.repository.datachangelog.plugin.DataChangeLogTableRegistry;
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
