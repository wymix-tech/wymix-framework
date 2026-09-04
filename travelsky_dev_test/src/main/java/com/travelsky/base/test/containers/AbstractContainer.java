package com.travelsky.base.test.containers;

import com.travelsky.base.test.configuration.JunitConfiguration;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.InputStream;
import java.util.Properties;

@Testcontainers
public abstract class AbstractContainer extends JunitConfiguration {

    public static final Properties UNIT_TEST_PROPERTIES;
    private static final String STRATEGY = "docker.client.strategy";
    private static final String DOCKER_HOST = "docker.host";

    static {
        Properties systemProperties = System.getProperties();
        UNIT_TEST_PROPERTIES = new Properties();
        try (InputStream in = ResourceLoader.class.getClassLoader().getResourceAsStream("unittest.properties")) {
            UNIT_TEST_PROPERTIES.load(in);
        } catch (Exception e) {
            // DO NOTHING
        }

        if (systemProperties.containsKey(STRATEGY)) {
            TestcontainersConfiguration.getInstance()
                    .updateUserConfig(STRATEGY, systemProperties.get(STRATEGY).toString());
        }
        if (systemProperties.containsKey(DOCKER_HOST)) {
            TestcontainersConfiguration.getInstance()
                    .updateUserConfig(DOCKER_HOST, systemProperties.get(DOCKER_HOST).toString());
        }
        if ((!systemProperties.containsKey(STRATEGY)) && UNIT_TEST_PROPERTIES.containsKey(STRATEGY)) {
            TestcontainersConfiguration.getInstance()
                    .updateUserConfig(STRATEGY, UNIT_TEST_PROPERTIES.get(STRATEGY).toString());
        }
        if ((!systemProperties.containsKey(DOCKER_HOST)) && UNIT_TEST_PROPERTIES.containsKey(DOCKER_HOST)) {
            TestcontainersConfiguration.getInstance()
                    .updateUserConfig(DOCKER_HOST, UNIT_TEST_PROPERTIES.get(DOCKER_HOST).toString());
        }
    }

}
