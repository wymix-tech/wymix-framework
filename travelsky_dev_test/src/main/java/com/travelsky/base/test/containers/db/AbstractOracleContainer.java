package com.travelsky.base.test.containers.db;


import org.testcontainers.containers.OracleContainer;

public abstract class AbstractOracleContainer extends AbstractDbContainer {

    private static final String DEFAULT_ORACLE_DOCKER_IMAGE_VERSION = "mysql:8.4.2";

    static {
        container = new OracleContainer(UNIT_TEST_PROPERTIES.getProperty("db.oracle.image", DEFAULT_ORACLE_DOCKER_IMAGE_VERSION))
                .withUsername(UNIT_TEST_PROPERTIES.getProperty("db.username", DEFAULT_USERNAME))
                .withPassword(UNIT_TEST_PROPERTIES.getProperty("db.password", DEFAULT_PASSWORD))
                .withDatabaseName(UNIT_TEST_PROPERTIES.getProperty("db.database", DEFAULT_DATABASE));
    }

}
