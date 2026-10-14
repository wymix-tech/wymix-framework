package org.wymix.framework.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class ClearOperation extends AbstractOperation {

    public ClearOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.DELETE_ALL.execute(connection, getDataSet());
    }
}
