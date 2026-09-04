package com.travelsky.base.test.containers.db;


import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPgSqlContainer extends AbstractDbContainer {

    private static final String DEFAULT_PG_DOCKER_IMAGE_VERSION = "mysql:8.4.2";

    static {
        container = new PostgreSQLContainer(UNIT_TEST_PROPERTIES.getProperty("db.pg.image", DEFAULT_PG_DOCKER_IMAGE_VERSION))
                .withUsername(UNIT_TEST_PROPERTIES.getProperty("db.username", DEFAULT_USERNAME))
                .withPassword(UNIT_TEST_PROPERTIES.getProperty("db.password", DEFAULT_PASSWORD))
                .withDatabaseName(UNIT_TEST_PROPERTIES.getProperty("db.database", DEFAULT_DATABASE));
    }
}
