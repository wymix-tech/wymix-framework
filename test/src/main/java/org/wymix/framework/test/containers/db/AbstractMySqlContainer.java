package org.wymix.framework.test.containers.db;

import org.testcontainers.containers.MySQLContainer;

public abstract class AbstractMySqlContainer extends AbstractDbContainer {

    private static final String DEFAULT_MYSQL_DOCKER_IMAGE_VERSION = "mysql:8.4.2";

    static {
        container = new MySQLContainer(UNIT_TEST_PROPERTIES.getProperty("db.mysql.image", DEFAULT_MYSQL_DOCKER_IMAGE_VERSION))
                .withUsername(UNIT_TEST_PROPERTIES.getProperty("db.username", DEFAULT_USERNAME))
                .withPassword(UNIT_TEST_PROPERTIES.getProperty("db.password", DEFAULT_PASSWORD))
                .withDatabaseName(UNIT_TEST_PROPERTIES.getProperty("db.database", DEFAULT_DATABASE));
    }

}
