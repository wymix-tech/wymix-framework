package org.wymix.framework.test.listener;

import org.wymix.framework.test.annotations.DataSet;
import org.wymix.framework.test.dbutil.operation.ClearOperation;
import org.wymix.framework.test.dbutil.operation.DeleteOperation;
import org.wymix.framework.test.dbutil.operation.InsertOperation;
import org.wymix.framework.test.dbutil.operation.RefreshOperation;
import org.wymix.framework.test.dbutil.operation.TruncateOperation;
import org.wymix.framework.test.dbutil.operation.UpdateOperation;
import org.wymix.framework.test.enums.Operation;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.mysql.MySqlConnection;
import org.dbunit.ext.oracle.OracleConnection;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataSetSupportListener implements BeforeTestExecutionCallback,
        AfterTestExecutionCallback, AfterAllCallback {

    private static final ConcurrentMap<Class<?>, String[]> LOADED_DATA = new ConcurrentHashMap<>();

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        List<String> dataFileSet = new ArrayList<>();
        extensionContext.getTestInstance().ifPresent(instance -> {
            Class<?> instanceClass = instance.getClass();
            String[] s = LOADED_DATA.get(instanceClass);
            if (s != null) {
                dataFileSet.addAll(Arrays.asList(s));
            }
        });

        extensionContext.getTestMethod().ifPresent(method -> {
            if (!method.isAnnotationPresent(DataSet.class)) {
                return;
            }
            DataSet data = method.getAnnotation(DataSet.class);
            try {
                loadData(data, data.teardown(), Arrays.stream(data.values()).filter(value -> !dataFileSet.contains(value)).toArray(String[]::new), extensionContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        List<String> dataFileSet = new ArrayList<>();
        extensionContext.getTestInstance().ifPresent(instance -> {
            Class<?> instanceClass = instance.getClass();
            String[] s = LOADED_DATA.get(instanceClass);
            if (s == null && instanceClass.isAnnotationPresent(DataSet.class)) {
                DataSet data = instanceClass.getAnnotation(DataSet.class);
                try {
                    loadData(data, data.setup(), data.values(), extensionContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                s = LOADED_DATA.computeIfAbsent(instanceClass, clazz -> data.values());
            }
            if (s != null) {
                dataFileSet.addAll(Arrays.asList(s));
            }
        });

        extensionContext.getTestMethod().ifPresent(method -> {
            if (!method.isAnnotationPresent(DataSet.class)) {
                return;
            }
            DataSet data = method.getAnnotation(DataSet.class);
            try {
                loadData(data, data.setup(), Arrays.stream(data.values()).filter(value -> !dataFileSet.contains(value)).toArray(String[]::new), extensionContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void loadData(DataSet data, Operation operation, String[] values, ExtensionContext extensionContext) throws Exception {
        if (values == null || values.length == 0) {
            return;
        }
        List<DataSource> retDataSource = new ArrayList<>();
        if (data.dataSourceNames().length == 1) {
            DataSource dataSource = SpringExtension.getApplicationContext(extensionContext).getBean(data.dataSourceNames()[0], DataSource.class);
            retDataSource.add(dataSource);
        } else {
            for (String dataSourceName : data.dataSourceNames()) {
                Map<String, DataSource> dataSources = SpringExtension.getApplicationContext(extensionContext).getBeansOfType(DataSource.class);
                DataSource dataSource = dataSources.get(dataSourceName);
                if (dataSource != null) {
                    retDataSource.add(dataSource);
                }
            }
        }
        if (retDataSource.size() > 1 && values.length != retDataSource.size()) {
            throw new IllegalArgumentException("values.length must be equal to dataSources.size()");
        }
        List<IDatabaseConnection> connections = createDatabaseConnection(retDataSource, data.database());
        switch (operation) {
            case INSERT:
                for (int i = 0; i < values.length; i++) {
                    new InsertOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            case UPDATE:
                for (int i = 0; i < values.length; i++) {
                    new UpdateOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            case DELETE:
                for (int i = 0; i < values.length; i++) {
                    new DeleteOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            case REFRESH:
                for (int i = 0; i < values.length; i++) {
                    new RefreshOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            case CLEAR:
                for (int i = 0; i < values.length; i++) {
                    new ClearOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            case TRUNCATE:
                for (int i = 0; i < values.length; i++) {
                    new TruncateOperation(connections.get(connections.size() == 1 ? 0 : i), values[i]).execute();
                }
                break;
            default:
                break;
        }
    }

    private List<IDatabaseConnection> createDatabaseConnection(List<DataSource> retDataSources, String schema) throws SQLException, DatabaseUnitException {
        List<IDatabaseConnection> ret = new ArrayList<>();
        for (DataSource retDataSource : retDataSources) {
            Connection connection = DataSourceUtils.getConnection(retDataSource);
            String databaseProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (databaseProductName.contains("mysql")) {
                ret.add(new MySqlConnection(connection, schema));
            } else if (databaseProductName.contains("oracle")) {
                ret.add(new OracleConnection(connection, schema));
            } else {
                ret.add(new DatabaseConnection(connection, schema));
            }
        }
        return ret;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        extensionContext.getTestInstance().ifPresent(instance -> {
            Class<?> instanceClass = instance.getClass();
            String[] s = LOADED_DATA.get(instanceClass);
            if (s != null) {
                DataSet data = instanceClass.getAnnotation(DataSet.class);
                try {
                    loadData(data, data.teardown(), data.values(), extensionContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                LOADED_DATA.remove(instanceClass);
            }
        });
    }
}
