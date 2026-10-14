package com.swcares.sample.configuration;

import org.wymix.framework.core.configuration.CustomFeignConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/*
 凡是远程调用都可以使用Feign来进行嗲用，避免手写HttpClient
 具体配置方法见application-feign.yml
 */
@Configuration
@EnableFeignClients(basePackages = {"com.swcares.sample.rpc"})
public class FeignConfiguration extends CustomFeignConfiguration {
}
