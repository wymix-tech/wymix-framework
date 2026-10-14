package org.wymix.framework.discovery.configuration;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Isolated management channel: cluster credentials never authorize business or other actuator
 * requests.
 */
@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(DecentralizedDiscoveryProperties.class)
@ConditionalOnProperty(prefix = "decentralization.discovery", name = "enable", havingValue = "true")
public class DiscoverySecurityConfiguration {
    @Bean
    @Order(-100)
    public SecurityFilterChain discoverySecurityFilterChain(
            HttpSecurity http, DecentralizedDiscoveryProperties properties) throws Exception {
        byte[] secret = properties.requireSharedSecret().getBytes(StandardCharsets.UTF_8);
        http.securityMatcher(
                EndpointRequest.to(
                        "nodeRegister",
                        "nodeRefresh",
                        "nodes",
                        "election",
                        "leaderHeartbeat",
                        "checkLeaderStatus",
                        "broadcast"));
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(r -> r.anyRequest().authenticated());
        http.addFilterBefore(
                new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(
                            jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpServletResponse response,
                            jakarta.servlet.FilterChain chain)
                            throws java.io.IOException, jakarta.servlet.ServletException {
                        String supplied = request.getHeader("X-Wymix-Cluster-Token");
                        if (supplied == null
                                || !MessageDigest.isEqual(
                                        secret, supplied.getBytes(StandardCharsets.UTF_8))) {
                            response.sendError(401);
                            return;
                        }
                        SecurityContextHolder.getContext()
                                .setAuthentication(
                                        new UsernamePasswordAuthenticationToken(
                                                "cluster", null, List.of()));
                        try {
                            chain.doFilter(request, response);
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }
                },
                UsernamePasswordAuthenticationFilter.class);
        return new ClusterSecurityFilterChain(http.build());
    }
}
