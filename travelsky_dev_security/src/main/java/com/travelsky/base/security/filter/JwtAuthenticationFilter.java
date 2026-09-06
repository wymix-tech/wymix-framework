package com.travelsky.base.security.filter;

import com.travelsky.base.common.context.IgnoreUrlContext;
import com.travelsky.base.common.context.Principal;
import com.travelsky.base.common.context.UserContextHolder;
import com.travelsky.base.security.context.JwtTokenBlackList;
import com.travelsky.base.security.helper.JwtTokenHelper;
import com.travelsky.base.security.properties.CustomSecurityProperties;
import com.travelsky.base.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenHelper jwtTokenHelper;

    private final JwtTokenBlackList jwtTokenBlackList;

    @Autowired(required = false)
    private List<IgnoreUrlContext> ignoreUrlContexts;
    @Autowired
    private CustomSecurityProperties customSecurityProperties;

    private volatile RequestMatcher[] requestMatchers;

    private final Object mutex = new Object();


    public JwtAuthenticationFilter(JwtTokenHelper jwtTokenHelper, JwtTokenBlackList jwtTokenBlackList) {
        this.jwtTokenHelper = jwtTokenHelper;
        this.jwtTokenBlackList = jwtTokenBlackList;
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


        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (StringUtils.isNotBlank(authorizationHeader) && !authorizationHeader.startsWith("Basic ")) {
            jwt = authorizationHeader;
            jwt = jwt.replaceFirst("Bearer ", "");
            username = jwtTokenHelper.extractUsername(jwt);
        }

        if (username != null && jwtTokenHelper.validateToken(jwt) && !jwtTokenBlackList.contains(jwt)) {
            Authentication authentication = jwtTokenHelper.extractAuthentication(jwt);
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(), null, authentication.getAuthorities());
            WebAuthenticationDetails details = new WebAuthenticationDetailsSource().buildDetails(request);
            usernamePasswordAuthenticationToken.setDetails(details);
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            Principal<UsernamePasswordAuthenticationToken> principal = new Principal<>();
            principal.setPrincipal(username);
            principal.setIpAddress(details.getRemoteAddress());
            principal.setAuthentication(usernamePasswordAuthenticationToken);
            UserContextHolder.setContext(principal);
        }
        try {
            chain.doFilter(request, response);
        }finally {
            UserContextHolder.clearContext();
            SecurityContextHolder.clearContext();
        }


    }
}
