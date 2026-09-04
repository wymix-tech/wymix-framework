package com.swcares.sample.configuration;

import com.travelsky.base.web.configuration.AbstractApiDocConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ApiDocConfiguration extends AbstractApiDocConfiguration {

    @Override
    protected String[] packageToScan() {
        return new String[]{"com.swcares.sample.web"};
    }
}
