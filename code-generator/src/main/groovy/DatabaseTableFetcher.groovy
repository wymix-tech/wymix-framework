import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DatabaseTableFetcher {
    private String dbUrl
    private String dbUser
    private String dbPassword

    DatabaseTableFetcher(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl
        this.dbUser = dbUser
        this.dbPassword = dbPassword
    }

    Connection getConnection() {
        DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    }

    List<String> getAllTables() {
        def metaData = getConnection().metaData
        def tables = []

        ResultSet resultSet = metaData.getTables(null, null, '%', ['TABLE'] as String[])
        while (resultSet.next()) {
            def tn = resultSet.getString("TABLE_NAME")
            def comment = resultSet.getString("REMARKS")

            tables << [
                    tableName: tn,
                    comment: comment
            ]
        }

        resultSet.close()
        return tables
    }

    List<Map<String, Object>> getTableDetails(String tableName) {
        def metaData = getConnection().metaData
        def columns = []

        // Get column details for the specified table
        ResultSet resultSet = metaData.getColumns(null, null, tableName, "%")
        while (resultSet.next()) {
            def columnName = resultSet.getString("COLUMN_NAME")
            def dataType = resultSet.getInt("DATA_TYPE")
            def typeName = resultSet.getString("TYPE_NAME")
            def size = resultSet.getInt("COLUMN_SIZE")
            def nullable = resultSet.getInt("NULLABLE") == 1 ? "YES" : "NO"
            def comment = resultSet.getString("REMARKS")

            columns << [
                    columnName: columnName,
                    dataType  : dataType,
                    typeName  : typeName,
                    size      : size,
                    nullable  : nullable,
                    comment   : comment
            ]
        }
        resultSet.close()

        return columns
    }

    List<String> getPrimaryKeys(String tableName) {
        def metaData = getConnection().metaData
        def primaryKeys = []

        // Get primary key details for the specified table
        ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName)
        while (resultSet.next()) {
            def primaryKeyColumn = resultSet.getString("COLUMN_NAME")
            primaryKeys.add(primaryKeyColumn)
        }
        resultSet.close()

        return primaryKeys
    }
}
