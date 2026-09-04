package com.swcares.sample.configuration;

import com.travelsky.base.security.configuration.CustomSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfiguration extends CustomSecurityConfiguration {

    // 在此注入UserDetailService实现即可，演示采用内存用户
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();
        inMemoryUserDetailsManager.createUser(
                User
                        .withUsername("admin")
                        .password("Abc@123")
                        .passwordEncoder(s -> passwordEncoder().encode(s))
                        .authorities("superadmin")
                        .build()
        );
        return inMemoryUserDetailsManager;
    }


}
