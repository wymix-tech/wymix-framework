package org.wymix.framework.core.configuration;

import org.wymix.framework.core.feign.GlobalLoadBalancerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;

@LoadBalancerClients(defaultConfiguration = GlobalLoadBalancerConfiguration.class)
public abstract class CustomFeignConfiguration {

    /**
     * Feign 响应解码器。
     *
     * <p>优先复用 Spring 容器中的 {@link ObjectMapper}：它已由 Boot 注册好
     * {@code JavaTimeModule} 等模块；而 {@code new JacksonDecoder()} 内部用的是裸 ObjectMapper，
     * 遇到 {@code LocalDateTime} 会直接抛 {@code Java 8 date/time type not supported by default}，
     * 节点信息这种带时间字段的响应会整体反序列化失败。
     */
    @Bean
    @ConditionalOnMissingBean(Decoder.class)
    public Decoder decoder(@Autowired(required = false) ObjectMapper objectMapper) {
        return objectMapper != null ? new JacksonDecoder(objectMapper) : new JacksonDecoder();
    }

}
