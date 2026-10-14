package org.wymix.framework.monitor.configuration;

import org.wymix.framework.common.context.IgnoreUrlContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MonitorConfiguration {

    @Bean
    public IgnoreUrlContext monitorIgnoreUrlContext() {
        IgnoreUrlContext ignoreUrlContext = new IgnoreUrlContext();
        ignoreUrlContext.setIgnoreUrls(new String[]{"/actuator/**"});
        return ignoreUrlContext;
    }
}
