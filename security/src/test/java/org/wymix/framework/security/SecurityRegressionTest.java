package org.wymix.framework.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.wymix.framework.security.context.JwtTokenBlackList;
import org.wymix.framework.security.customizer.SecurityCustomizer;
import org.wymix.framework.security.helper.JwtTokenHelper;

import java.util.List;

@SpringBootTest(
        classes = SecurityRegressionTest.Application.class,
        properties = {
            "security.jwt.secret=01234567890123456789012345678901",
                    "security.jwt.expired-time=3600",
            "security.form-login-enable=false",
                    "management.endpoints.web.exposure.include=health,info"
        })
@AutoConfigureMockMvc
class SecurityRegressionTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        Controller controller() {
            return new Controller();
        }

        @Bean
        SecurityCustomizer customizer() {
            return http -> {
                try {
                    http.headers(
                            headers ->
                                    headers.addHeaderWriter(
                                            (request, response) ->
                                                    response.setHeader("X-Customizer", "active")));
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            };
        }
    }

    @RestController
    static class Controller {
        @GetMapping("/secured")
        String secured() {
            return "ok";
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JwtTokenHelper tokens;
    @Autowired JwtTokenBlackList blacklist;

    String token(String role) {
        return tokens.generateToken(
                new UsernamePasswordAuthenticationToken(
                        "user", null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void disablingFormLoginRetainsJwtAuthorizationAndCustomizer() throws Exception {
        mvc.perform(get("/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Customizer", "active"));
        mvc.perform(get("/secured").header("Authorization", "Bearer " + token("ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidExpiredOrRevokedCredentialsReturn401() throws Exception {
        mvc.perform(get("/secured").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        String jwt = token("ROLE_USER");
        blacklist.addUntil(jwt, tokens.extractExpiration(jwt).getTime());
        mvc.perform(get("/secured").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
        String expired =
                io.jsonwebtoken.Jwts.builder()
                        .subject("user")
                        .expiration(new java.util.Date(0))
                        .signWith(
                                io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                        "01234567890123456789012345678901"
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .compact();
        mvc.perform(get("/secured").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managementEndpointsRequireAdminEvenForAuthenticatedUsers() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info").header("Authorization", "Bearer " + token("ROLE_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/actuator/info")
                                .header("Authorization", "Bearer " + token("ROLE_ACTUATOR_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @SuppressWarnings("unchecked")
    void logoutRevokesUntilSignedExpiration() throws Exception {
        String jwt = token("ROLE_USER");
        long expiration = tokens.extractExpiration(jwt).getTime();
        mvc.perform(post("/logout").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        var entries =
                (java.util.Map<String, Long>)
                        org.springframework.test.util.ReflectionTestUtils.getField(
                                blacklist, "TOKEN_BLACK_LIST");
        assertEquals(expiration, entries.get(jwt), 50L);
        mvc.perform(get("/secured").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingSecretHasReadableErrorAndInstancesDoNotShareBlacklists() {
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> new JwtTokenHelper(null, 3600L))
                        .getMessage()
                        .contains("security.jwt.secret"));
        JwtTokenBlackList first = new JwtTokenBlackList(), second = new JwtTokenBlackList();
        first.add("x", 1000);
        assertTrue(first.contains("x"));
        assertFalse(second.contains("x"));
    }

    @Test
    void newlyIssuedTokensAreUniqueAndNotAffectedByEarlierLogout() throws Exception {
        String previous = token("ROLE_USER"), fresh = token("ROLE_USER");
        assertNotEquals(previous, fresh);
        blacklist.addUntil(previous, tokens.extractExpiration(previous).getTime());
        mvc.perform(get("/secured").header("Authorization", "Bearer " + fresh))
                .andExpect(status().isOk());
    }
}
