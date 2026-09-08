package com.travelsky.base.security.configuration;

import com.travelsky.base.common.context.IgnoreUrlContext;
import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.security.context.JwtTokenBlackList;
import com.travelsky.base.security.customizer.SecurityCustomizer;
import com.travelsky.base.security.entrypoint.Http401UnauthorizedEntryPoint;
import com.travelsky.base.security.filter.JwtAuthenticationFilter;
import com.travelsky.base.security.handler.BaseAccessDeniedHandler;
import com.travelsky.base.security.handler.InvalidJwtTokenLogoutHandler;
import com.travelsky.base.security.handler.LoginFailureHandler;
import com.travelsky.base.security.handler.LoginSuccessHandler;
import com.travelsky.base.security.helper.JwtTokenHelper;
import com.travelsky.base.security.properties.CustomSecurityProperties;
import com.travelsky.base.utils.HttpUtils;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安全自动装配。
 *
 * <p>说明：{@code @AutoConfiguration} 默认 {@code proxyBeanMethods=false}，因此本类内部
 * <b>不能</b>通过调用 {@code @Bean} 方法获取依赖（每次调用都会 new 出新实例，导致
 * 黑名单等有状态 Bean 出现多份）。所有依赖统一改为方法参数注入。
 * 同理，处理器类一律用构造器注入依赖，不能用 {@code @Autowired} 字段 + {@code new} 创建。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSecurity
@EnableConfigurationProperties(CustomSecurityProperties.class)
public class CustomSecurityConfiguration {

    static {
        System.setProperty("security", "true");
    }

    @Autowired(required = false)
    private List<IgnoreUrlContext> ignoreUrlContexts;

    @Autowired(required = false)
    private List<SecurityCustomizer> securityCustomizers;

    @Bean
    public JwtTokenHelper jwtTokenHelper(@Autowired CustomSecurityProperties customSecurityProperties) {
        boolean randomSecret = customSecurityProperties.getJwt().isRandomSecret();
        if (randomSecret) {
            customSecurityProperties.getJwt().setSecret(StringUtils.generateRandomString(32));
        }
        return new JwtTokenHelper(customSecurityProperties.getJwt().getSecret(), customSecurityProperties.getJwt().getExpiredTime());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Autowired JwtTokenHelper jwtTokenHelper,
                                                           @Autowired JwtTokenBlackList jwtTokenBlackList,
                                                           @Autowired CustomSecurityProperties customSecurityProperties) {
        return new JwtAuthenticationFilter(jwtTokenHelper, jwtTokenBlackList, ignoreUrlContexts, customSecurityProperties);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new Http401UnauthorizedEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationSuccessHandler.class)
    public AuthenticationSuccessHandler authenticationSuccessHandler(@Autowired JwtTokenHelper jwtTokenHelper) {
        return new LoginSuccessHandler(jwtTokenHelper);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationFailureHandler.class)
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new LoginFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler accessDeniedHandler() {
        return new BaseAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnMissingBean(value = {LogoutHandler.class}, name = {"invalidJwtTokenLogoutHandler"})
    public LogoutHandler invalidJwtTokenLogoutHandler(@Autowired JwtTokenHelper jwtTokenHelper,
                                                      @Autowired JwtTokenBlackList jwtTokenBlackList) {
        return new InvalidJwtTokenLogoutHandler(jwtTokenHelper, jwtTokenBlackList);
    }

    @Bean
    @ConditionalOnMissingBean(value = {JwtTokenBlackList.class})
    public JwtTokenBlackList jwtTokenBlackList() {
        return new JwtTokenBlackList();
    }

    @Bean
    @ConditionalOnMissingBean(value = {LogoutSuccessHandler.class})
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> HttpUtils.response(response, 200, JsonUtils.entityToJson(ResultVO.of(200, "logout.success")));
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        String encodingId = "bcrypt";
        BCryptPasswordEncoder cryptPasswordEncoder = new BCryptPasswordEncoder();
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", cryptPasswordEncoder);
//        encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
//        encoders.put("scrypt", new SCryptPasswordEncoder());
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(encodingId, encoders);
        passwordEncoder.setDefaultPasswordEncoderForMatches(cryptPasswordEncoder);
        return passwordEncoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            @Autowired CustomSecurityProperties customSecurityProperties,
                                            @Autowired JwtAuthenticationFilter jwtAuthenticationFilter,
                                            @Autowired AuthenticationSuccessHandler authenticationSuccessHandler,
                                            @Autowired AuthenticationFailureHandler authenticationFailureHandler,
                                            @Autowired AccessDeniedHandler accessDeniedHandler,
                                            @Autowired AuthenticationEntryPoint authenticationEntryPoint,
                                            @Autowired @Qualifier("invalidJwtTokenLogoutHandler") LogoutHandler invalidJwtTokenLogoutHandler,
                                            @Autowired LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        if (!customSecurityProperties.isFormLoginEnable()) {
            httpSecurity.authorizeHttpRequests(e -> {
                e.requestMatchers("/**").permitAll().anyRequest().authenticated();
            });
            httpSecurity.formLogin(AbstractHttpConfigurer::disable);
            httpSecurity.httpBasic(AbstractHttpConfigurer::disable);
            httpSecurity.csrf(AbstractHttpConfigurer::disable);
            return httpSecurity.build();
        }
        List<String> ignoreUrls = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ignoreUrlContexts)) {
            for (IgnoreUrlContext ignoreUrlContext : ignoreUrlContexts) {
                ignoreUrls.addAll(Arrays.asList(ignoreUrlContext.getIgnoreUrls()));
            }
        }
        if (StringUtils.isNotBlank(customSecurityProperties.getIgnoreUrls())) {
            ignoreUrls.addAll(Arrays.stream(customSecurityProperties.getIgnoreUrls().split(",")).map(StringUtils::trim).toList());
        }
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(r -> {
                    r.requestMatchers(ignoreUrls.toArray(String[]::new)).permitAll()
                            .anyRequest().authenticated();
                });
        httpSecurity.formLogin(formLoginConfigurer -> {
            if (customSecurityProperties.isFormLoginEnable()) {
                formLoginConfigurer
                        .loginProcessingUrl(customSecurityProperties.getLoginProcessingUrl()).permitAll()
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler);
                if (StringUtils.isNotBlank(customSecurityProperties.getLoginPage())) {
                    formLoginConfigurer.loginPage(customSecurityProperties.getLoginPage()).permitAll();
                }
            }
        });


        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint));


        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);

        httpSecurity.logout(logoutConfigurer ->
                logoutConfigurer
                        .logoutUrl(customSecurityProperties.getLogoutUrl())
                        .addLogoutHandler(invalidJwtTokenLogoutHandler)
                        .logoutSuccessHandler(logoutSuccessHandler)
        );

        httpSecurity.sessionManagement(configure -> {
            configure.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });


        if (!CollectionUtils.isEmpty(this.securityCustomizers)) {
            for (SecurityCustomizer securityCustomizer : this.securityCustomizers) {
                securityCustomizer.customize(httpSecurity);
            }
        }

        return httpSecurity.build();
    }

}
