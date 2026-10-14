package org.wymix.framework.test.dbutil.operation;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.DatabaseOperation;

public class InsertOperation extends AbstractOperation {

    public InsertOperation(IDatabaseConnection connection, String fileName) {
        super(connection, fileName);
    }

    @Override
    public void execute() throws Exception {
        DatabaseOperation.INSERT.execute(connection, getDataSet());
    }
}
