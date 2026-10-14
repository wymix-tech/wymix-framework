package org.wymix.framework.security.filter;

import org.wymix.framework.common.context.IgnoreUrlContext;
import org.wymix.framework.common.context.Principal;
import org.wymix.framework.common.context.UserContextHolder;
import org.wymix.framework.security.context.JwtTokenBlackList;
import org.wymix.framework.security.helper.JwtTokenHelper;
import org.wymix.framework.security.properties.CustomSecurityProperties;
import org.wymix.framework.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint =
            new org.wymix.framework.security.entrypoint.Http401UnauthorizedEntryPoint();

    public void setAuthenticationEntryPoint(org.springframework.security.web.AuthenticationEntryPoint entryPoint) {
        this.authenticationEntryPoint = java.util.Objects.requireNonNull(entryPoint);
    }

    private final JwtTokenHelper jwtTokenHelper;

    private final JwtTokenBlackList jwtTokenBlackList;

    /**
     * 白名单上下文：由构造器传入（本类由配置类用 {@code new} 创建，字段注入不会生效）。
     */
    private final List<IgnoreUrlContext> ignoreUrlContexts;

    private final CustomSecurityProperties customSecurityProperties;

    private volatile RequestMatcher[] requestMatchers;

    private final Object mutex = new Object();


    public JwtAuthenticationFilter(JwtTokenHelper jwtTokenHelper, JwtTokenBlackList jwtTokenBlackList) {
        this(jwtTokenHelper, jwtTokenBlackList, null, null);
    }

    public JwtAuthenticationFilter(JwtTokenHelper jwtTokenHelper,
                                   JwtTokenBlackList jwtTokenBlackList,
                                   List<IgnoreUrlContext> ignoreUrlContexts,
                                   CustomSecurityProperties customSecurityProperties) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.jwtTokenBlackList = jwtTokenBlackList;
        this.ignoreUrlContexts = ignoreUrlContexts;
        this.customSecurityProperties = customSecurityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (this.requestMatchers == null) {
            synchronized (this.mutex) {
                if (this.requestMatchers == null) {
                    List<String> ignoreUrls = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(ignoreUrlContexts)) {
                        for (IgnoreUrlContext ignoreUrlContext : ignoreUrlContexts) {
                            ignoreUrls.addAll(Arrays.asList(ignoreUrlContext.getIgnoreUrls()));
                        }
                    }
                    // 该 Filter 可能由非 Spring 托管的方式实例化，属性可能为空，需判空
                    if (customSecurityProperties != null && StringUtils.isNotBlank(customSecurityProperties.getIgnoreUrls())) {
                        ignoreUrls.addAll(Arrays.stream(customSecurityProperties.getIgnoreUrls().split(",")).map(StringUtils::trim).toList());
                    }
                    this.requestMatchers = ignoreUrls.stream().map(url-> PathPatternRequestMatcher.withDefaults().matcher(url)).toArray(RequestMatcher[]::new);
                }
            }
        }
        if (requestMatchers != null && requestMatchers.length > 0) {
            for (RequestMatcher requestMatcher : requestMatchers) {
                if (requestMatcher.matches(request)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }


        try {
            String jwt = bearerToken(request.getHeader("Authorization"));
            if (jwt != null) {
                try {
                    if (jwtTokenBlackList.contains(jwt)) {
                        throw new IllegalArgumentException("Revoked JWT");
                    }
                    Authentication authentication = jwtTokenHelper.extractAuthentication(jwt);
                    WebAuthenticationDetails details = new WebAuthenticationDetailsSource().buildDetails(request);
                    ((UsernamePasswordAuthenticationToken) authentication).setDetails(details);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    Principal<Authentication> principal = new Principal<>();
                    principal.setPrincipal(authentication.getName());
                    principal.setIpAddress(details.getRemoteAddress());
                    principal.setAuthentication(authentication);
                    UserContextHolder.setContext(principal);
                } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(request, response,
                            new org.springframework.security.authentication.BadCredentialsException("Invalid bearer token", ex));
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clearContext();
            SecurityContextHolder.clearContext();
        }
    }

    /** Only Bearer credentials are accepted; arbitrary Authorization values are not JWTs. */
    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
