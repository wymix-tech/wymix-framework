package org.wymix.framework.monitor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.mock.env.MockEnvironment;
import org.wymix.framework.monitor.listener.MonitorApplicationStartedEventListener;

class MonitorDefaultsTest {
    @Test
    void doesNotExposeAllEndpointsAndPreservesOverrides() {
        var env = new MockEnvironment();
        var listener = new MonitorApplicationStartedEventListener();
        listener.onApplicationEvent(
                new ApplicationEnvironmentPreparedEvent(
                        new DefaultBootstrapContext(),
                        new SpringApplication(MonitorDefaultsTest.class),
                        new String[0],
                        env));
        assertEquals(
                "health,info,prometheus",
                env.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("when_authorized", env.getProperty("management.endpoint.health.showDetails"));
        env.withProperty("management.endpoints.web.exposure.include", "health");
        assertEquals("health", env.getProperty("management.endpoints.web.exposure.include"));
    }
}
