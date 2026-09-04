package com.swcares.sample.boot;

import com.travelsky.base.core.boot.BootLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.swcares.sample")
public class ServerRunner {

    public static void main(String[] args) {
        SpringApplication springApplication;
        // 无需监控
        springApplication= BootLoader.genApp(ServerRunner.class);
        // 监控
        //springApplication= MonitorBootLoader.genApp(ServerRunner.class);
        springApplication.run(args);
    }
}
