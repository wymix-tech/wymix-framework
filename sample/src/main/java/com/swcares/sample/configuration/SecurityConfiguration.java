package com.swcares.sample.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * 新版引入方式：无需继承 CustomSecurityConfiguration，
 * 只需提供 UserDetailsService Bean 即启用表单登录。
 */
@Configuration
public class SecurityConfiguration {

    private final PasswordEncoder passwordEncoder;

    public SecurityConfiguration(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // 在此注入UserDetailService实现即可，演示采用内存用户
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();
        inMemoryUserDetailsManager.createUser(
                User
                        .withUsername("admin")
                        .password("Abc@123")
                        .passwordEncoder(s -> passwordEncoder.encode(s))
                        .authorities("superadmin")
                        .build()
        );
        return inMemoryUserDetailsManager;
    }


}
