package org.wymix.framework.discovery.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = DiscoverySecurityTest.Application.class,
        properties = {
            "decentralization.discovery.enable=true",
            "decentralization.discovery.shared-secret=01234567890123456789012345678901",
            "management.endpoints.web.exposure.include=nodeRegister,info"
        })
@AutoConfigureMockMvc
class DiscoverySecurityTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DecentralizedDiscoveryConfiguration.class)
    static class Application {
        @Bean
        Register endpoint() {
            return new Register();
        }

        @Bean
        @Order(100)
        SecurityFilterChain business(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(r -> r.anyRequest().authenticated())
                    .exceptionHandling(
                            e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
                    .build();
        }
    }

    @Endpoint(id = "nodeRegister")
    static class Register {
        @WriteOperation
        public String register() {
            return "ok";
        }
    }

    @Autowired MockMvc mvc;

    @Test
    void clusterChannelRequiresSecretAndDoesNotAuthorizeOtherEndpoints() throws Exception {
        mvc.perform(post("/actuator/nodeRegister")).andExpect(status().isUnauthorized());
        mvc.perform(post("/actuator/nodeRegister").header("X-Wymix-Cluster-Token", "bad"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/actuator/nodeRegister")
                                .header(
                                        "X-Wymix-Cluster-Token",
                                        "01234567890123456789012345678901"))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/actuator/info")
                                .header(
                                        "X-Wymix-Cluster-Token",
                                        "01234567890123456789012345678901"))
                .andExpect(status().isUnauthorized());
    }
}
