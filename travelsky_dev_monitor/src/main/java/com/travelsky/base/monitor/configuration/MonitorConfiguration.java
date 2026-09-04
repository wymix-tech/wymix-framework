package com.travelsky.base.monitor.configuration;

import com.travelsky.base.common.context.IgnoreUrlContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitorConfiguration {

    @Bean
    public IgnoreUrlContext monitorIgnoreUrlContext() {
        IgnoreUrlContext ignoreUrlContext = new IgnoreUrlContext();
        ignoreUrlContext.setIgnoreUrls(new String[]{"/actuator/**"});
        return ignoreUrlContext;
    }
}
