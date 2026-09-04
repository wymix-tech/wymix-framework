package com.travelsky.base.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class RefreshOperation extends AbstractOperation {

    public RefreshOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.REFRESH.execute(connection, getDataSet());
    }
}
