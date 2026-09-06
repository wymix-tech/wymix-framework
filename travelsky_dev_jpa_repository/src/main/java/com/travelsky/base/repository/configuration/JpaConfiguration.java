package com.travelsky.base.repository.configuration;

import com.travelsky.base.repository.audit.supports.AuditHandler;
import com.travelsky.base.repository.audit.supports.AuditListener;
import com.travelsky.base.repository.audit.supports.DatabaseAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "travelsky.jpa", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(AuditListener.class)
@Configuration
@DependsOn("entityManagerFactory")
public class JpaConfiguration {


    @Bean
    public DatabaseAuditorAware auditorAware() {
        return new DatabaseAuditorAware();
    }

    @Bean
    public AuditHandler auditHandler() {
        return new AuditHandler();
    }


}
