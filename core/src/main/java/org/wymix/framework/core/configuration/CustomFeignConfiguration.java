package org.wymix.framework.core.configuration;

import org.wymix.framework.core.feign.GlobalLoadBalancerConfiguration;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;

@LoadBalancerClients(defaultConfiguration = GlobalLoadBalancerConfiguration.class)
public abstract class CustomFeignConfiguration {

    @Bean
    @ConditionalOnMissingBean(Decoder.class)
    public Decoder decoder() {
        return new JacksonDecoder();
    }

}
