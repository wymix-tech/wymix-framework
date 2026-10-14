package org.wymix.framework.security.configuration;

import org.wymix.framework.common.context.IgnoreUrlContext;
import org.wymix.framework.common.result.ResultVO;
import org.wymix.framework.security.context.JwtTokenBlackList;
import org.wymix.framework.security.customizer.SecurityCustomizer;
import org.wymix.framework.security.entrypoint.Http401UnauthorizedEntryPoint;
import org.wymix.framework.security.filter.JwtAuthenticationFilter;
import org.wymix.framework.security.handler.BaseAccessDeniedHandler;
import org.wymix.framework.security.handler.InvalidJwtTokenLogoutHandler;
import org.wymix.framework.security.handler.LoginFailureHandler;
import org.wymix.framework.security.handler.LoginSuccessHandler;
import org.wymix.framework.security.helper.JwtTokenHelper;
import org.wymix.framework.security.properties.CustomSecurityProperties;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.utils.json.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
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
 *
 * <p><b>装配顺序（不要删除）</b>：Boot 的 {@code AutoConfigurationSorter} 先按类名字母序排，
 * {@code org.springframework.boot.*} 会整体排在 {@code org.wymix.*} 之前；若本类排在
 * {@code SpringBootWebSecurityConfiguration} / {@code ManagementWebSecurityAutoConfiguration} 之后，
 * 二者的 {@code @ConditionalOnDefaultWebSecurity}（无 {@code SecurityFilterChain} Bean 才生效）会先判定通过，
 * 于是 Boot 默认链与这里的 {@code securityFilterChain} 同时存在且都匹配任意请求，
 * 启动时抛 {@code UnreachableFilterChainException}。
 * 声明 {@code @AutoConfigureOrder(HIGHEST_PRECEDENCE)} 可让本类先于二者注册，使其条件正确失效。
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSecurity
@EnableConfigurationProperties(CustomSecurityProperties.class)
public class CustomSecurityConfiguration {

    @Autowired(required = false)
    private List<IgnoreUrlContext> ignoreUrlContexts;

    @Autowired(required = false)
    private List<SecurityCustomizer> securityCustomizers;

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenHelper jwtTokenHelper(@Autowired CustomSecurityProperties customSecurityProperties) {
        if (customSecurityProperties.getJwt() == null) {
            throw new IllegalArgumentException("security.jwt configuration is required");
        }
        boolean randomSecret = customSecurityProperties.getJwt().isRandomSecret();
        if (randomSecret) {
            customSecurityProperties.getJwt().setSecret(StringUtils.generateRandomString(32));
        }
        return new JwtTokenHelper(customSecurityProperties.getJwt().getSecret(), customSecurityProperties.getJwt().getExpiredTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Autowired JwtTokenHelper jwtTokenHelper,
                                                           @Autowired JwtTokenBlackList jwtTokenBlackList,
                                                           @Autowired CustomSecurityProperties customSecurityProperties,
                                                           AuthenticationEntryPoint authenticationEntryPoint) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenHelper, jwtTokenBlackList, ignoreUrlContexts, customSecurityProperties);
        filter.setAuthenticationEntryPoint(authenticationEntryPoint);
        return filter;
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
    @ConditionalOnMissingBean(name = "invalidJwtTokenLogoutHandler")
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
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(encodingId, encoders);
        passwordEncoder.setDefaultPasswordEncoderForMatches(cryptPasswordEncoder);
        return passwordEncoder;
    }

    @Bean
    @ConditionalOnMissingBean(value = SecurityFilterChain.class, ignoredType = "org.wymix.framework.discovery.configuration.ClusterSecurityFilterChain")
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            @Autowired CustomSecurityProperties customSecurityProperties,
                                            @Autowired JwtAuthenticationFilter jwtAuthenticationFilter,
                                            @Autowired AuthenticationSuccessHandler authenticationSuccessHandler,
                                            @Autowired AuthenticationFailureHandler authenticationFailureHandler,
                                            @Autowired AccessDeniedHandler accessDeniedHandler,
                                            @Autowired AuthenticationEntryPoint authenticationEntryPoint,
                                            @Autowired @Qualifier("invalidJwtTokenLogoutHandler") LogoutHandler invalidJwtTokenLogoutHandler,
                                            @Autowired LogoutSuccessHandler logoutSuccessHandler) throws Exception {
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
                    if (org.springframework.util.ClassUtils.isPresent("org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest", getClass().getClassLoader())) {
                        ActuatorRules.configure(r);
                    }
                    r.requestMatchers(ignoreUrls.toArray(String[]::new)).permitAll()
                            .anyRequest().authenticated();
                });
        if (customSecurityProperties.isFormLoginEnable()) {
            httpSecurity.formLogin(form -> {
                form.loginProcessingUrl(customSecurityProperties.getLoginProcessingUrl()).permitAll()
                        .successHandler(authenticationSuccessHandler).failureHandler(authenticationFailureHandler);
                if (StringUtils.isNotBlank(customSecurityProperties.getLoginPage())) {
                    form.loginPage(customSecurityProperties.getLoginPage()).permitAll();
                }
            });
        } else {
            httpSecurity.formLogin(AbstractHttpConfigurer::disable);
        }


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

    /** Prevent Servlet auto-registration: this filter belongs only to the security chain. */
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static final class ActuatorRules {
        static void configure(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
            rules.requestMatchers(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to("health")).permitAll();
            rules.requestMatchers(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR_ADMIN");
        }
    }

}
