package com.travelsky.base.data.migration.configuration;

import com.travelsky.base.data.migration.listener.DataMigrationEventListener;
import com.travelsky.base.repository.datachangelog.configuration.DataChangeLogConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DataChangeLogConfiguration.class})
public class DataMigrationConfiguration {

    @Bean
    private DataMigrationEventListener dataMigrationEventListener() {
        return new DataMigrationEventListener();
    }

}
