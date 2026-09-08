package com.swcares.sample.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类：直接使用 Spring Boot 原生启动方式。
 *
 * <p>框架的启动监听器（日志 / Feign 动态实例 / Actuator 默认值）已在各模块的
 * {@code META-INF/spring.factories} 中声明，由 {@code SpringApplication} 自动加载，
 * 因此无需再使用框架自定义的 BootLoader。
 */
@SpringBootApplication(scanBasePackages = "com.swcares.sample")
public class ServerRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServerRunner.class, args);
    }
}
