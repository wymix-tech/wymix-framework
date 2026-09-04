package com.travelsky.base.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class DeleteOperation extends AbstractOperation {

    public DeleteOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.DELETE.execute(connection, getDataSet());
    }
}
