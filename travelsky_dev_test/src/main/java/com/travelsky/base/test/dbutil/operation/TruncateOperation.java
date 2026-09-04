package com.travelsky.base.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class TruncateOperation extends AbstractOperation {

    public TruncateOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.TRUNCATE_TABLE.execute(connection, getDataSet());
    }
}
