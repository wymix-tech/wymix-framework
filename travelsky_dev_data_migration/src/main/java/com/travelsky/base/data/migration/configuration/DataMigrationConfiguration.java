package com.travelsky.base.data.migration.configuration;

import com.travelsky.base.data.migration.listener.DataMigrationEventListener;
import com.travelsky.base.repository.jpa.datachangelog.configuration.DataChangeLogConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "travelsky.data-migration", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@Import({DataChangeLogConfiguration.class})
public class DataMigrationConfiguration {

    @Bean
    public DataMigrationEventListener dataMigrationEventListener() {
        return new DataMigrationEventListener();
    }

}
