package ${configPackage};

import com.travelsky.base.security.customizer.SecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * 安全模块接入配置。
 *
 * <p>travelsky_dev_security 的 CustomSecurityConfiguration 已由
 * META-INF/spring/...AutoConfiguration.imports 自动装配，无需继承任何框架基类；
 * 业务方只需提供 UserDetailsService Bean 作为用户来源即可启用表单登录。
 *
 * <p>生产环境请替换为数据库实现（实现 UserDetailsService 并注册为 Bean），
 * 并删除下方基于内存用户的 InMemoryUserDetailsManager 示例。
 */
@Configuration
public class SecurityUserDetailsConfig {

    private final PasswordEncoder passwordEncoder;

    public SecurityUserDetailsConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户来源：此处以内存用户演示，请替换为真实数据源。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(
                User.withUsername("admin")
                        .password("ChangeMe@123")
                        .passwordEncoder(passwordEncoder::encode)
                        .authorities("admin")
                        .build()
        );
        return manager;
    }

    /**
     * 可选扩展：定制 HttpSecurity（权限规则、OAuth2 等）。
     * 声明 SecurityCustomizer Bean 即被 CustomSecurityConfiguration#securityFilterChain 收集调用；
     * 不需要定制时删除本 Bean 即可。
     */
    @Bean
    public SecurityCustomizer securityCustomizer() {
        return httpSecurity -> {
            // 示例：追加业务自定义授权规则
            // httpSecurity.authorizeHttpRequests(r -> r.requestMatchers("/public/**").permitAll());
        };
    }
}
