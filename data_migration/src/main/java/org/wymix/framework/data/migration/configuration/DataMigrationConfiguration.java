package org.wymix.framework.data.migration.configuration;

import org.wymix.framework.data.migration.listener.DataMigrationEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Event-only SPI consumer; does not select or import any ORM implementation. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.data-migration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataMigrationConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DataMigrationEventListener dataMigrationEventListener() { return new DataMigrationEventListener(); }
}
