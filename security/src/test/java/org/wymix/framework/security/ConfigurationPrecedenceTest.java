package org.wymix.framework.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.*;
import org.wymix.framework.security.handler.DecryptEnvironmentPostProcessor;

import java.util.Map;

class ConfigurationPrecedenceTest {
    @Test
    void preservesPlainAndEncodedPropertySourcePriority() {
        StandardEnvironment environment = new StandardEnvironment();
        environment
                .getPropertySources()
                .addFirst(
                        new MapPropertySource(
                                "high", Map.of("plain", "new", "encoded", "ENC(bmV3)")));
        environment
                .getPropertySources()
                .addLast(
                        new MapPropertySource(
                                "low", Map.of("plain", "ENC(b2xk)", "encoded", "ENC(b2xk)")));
        new DecryptEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        assertEquals("new", environment.getProperty("plain"));
        assertEquals("new", environment.getProperty("encoded"));
    }
}
