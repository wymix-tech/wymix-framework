class Tools {
    def static convertToCamelCase(snakeCase) {
        if (!snakeCase) return snakeCase

        def parts = snakeCase.split('_')
        def i = 0
        def camelCase = parts.collect { part ->
            if (i++ == 0) {
                part.toLowerCase()
            } else {
                caseFirstLetter(part)
            }
        }.join('')

        return camelCase
    }

    def static caseFirstLetter(String input, boolean capitalize = true) {
        if (!input) return input

        return (capitalize ? input.charAt(0).toUpperCase() : input.charAt(0).toLowerCase()).toString() + input.substring(1)
    }

    def static mapSqlTypeToJavaType(sqlType, size, dataType) {
        switch (sqlType.toUpperCase()) {
            case ["VARCHAR", "CHAR", "TEXT", "NVARCHAR2", "CLOB", "VARCHAR2"]:
                return "String"
            case ["INTEGER", "INT"]:
                return "Integer"
            case ["BIGINT", "LONG"]:
                return "Long"
            case ["SMALLINT", "SHORT"]:
                return "Short"
            case ["TINYINT", "BYTE"]:
                return "Byte"
            case ["FLOAT", "REAL"]:
                return "Float"
            case ["DOUBLE", "DOUBLE PRECISION", "BINARY_DOUBLE"]:
                return "Double"
            case ["DECIMAL", "NUMERIC", "NUMBER", "NUMBER(*)"]:
                if (dataType > 0)
                    return "BigDecimal"
                if (size < 11)
                    return "Integer"
                if (size < 20)
                    return "Long"
                return "BigDecimal"
            case ["BOOLEAN", "BIT"]:
                return "Boolean"
            case ["DATE", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITHOUT TIME ZONE", "TIMESTAMP(6)"]:
                return "Date"
            case ["TIME", "TIME WITH TIME ZONE", "TIME WITHOUT TIME ZONE"]:
                return "Time"
            case ["BLOB", "RAW"]:
                return "byte[]"
            case ["ARRAY"]:
                return "List<Object>"
            default:
                return "Object"
        }
    }

    def static writeToFile(className, content, path) {
        File dir = new File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        String filePath = "${path}/${className}.java"
        new FileWriter(filePath).withWriter { writer ->
            writer.write content
        }
    }
}
