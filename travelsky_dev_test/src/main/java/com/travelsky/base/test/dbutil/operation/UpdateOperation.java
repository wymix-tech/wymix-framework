package com.travelsky.base.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class UpdateOperation extends AbstractOperation {

    public UpdateOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.UPDATE.execute(connection, getDataSet());
    }
}
