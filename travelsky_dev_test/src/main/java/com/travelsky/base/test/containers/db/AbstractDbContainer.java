package com.travelsky.base.test.containers.db;

import com.travelsky.base.test.containers.AbstractContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;

@Testcontainers
public abstract class AbstractDbContainer extends AbstractContainer {

    protected static final String DEFAULT_USERNAME = "unittest";

    protected static final String DEFAULT_PASSWORD = "unit@Test4321";

    protected static final String DEFAULT_INIT_SQL_PATH = "schema/";

    protected static final String DEFAULT_DATABASE = "test";

    protected static JdbcDatabaseContainer<?> container;

    @DynamicPropertySource
    static void addProperties(DynamicPropertyRegistry registry) throws IOException, ScriptException {
        JdbcDatabaseDelegate jdbcDatabaseDelegate = new JdbcDatabaseDelegate(container, "");
        File sqlDir = new File(UNIT_TEST_PROPERTIES.getProperty("init.sql.path", DEFAULT_INIT_SQL_PATH));
        if (sqlDir.exists()) {
            for (String s : sqlDir.list()) {
                if (s.endsWith(".sql")) {
                    ScriptUtils.executeDatabaseScript(jdbcDatabaseDelegate, UNIT_TEST_PROPERTIES.getProperty("init.sql.path", DEFAULT_INIT_SQL_PATH), s);
                }
            }
        }
        container.start();
        registry.add("spring.datasource.druid.url", container::getJdbcUrl);
        registry.add("spring.datasource.druid.username", container::getUsername);
        registry.add("spring.datasource.druid.password", container::getPassword);
    }


}
