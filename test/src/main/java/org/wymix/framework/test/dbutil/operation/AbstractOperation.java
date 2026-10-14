package org.wymix.framework.test.dbutil.operation;

import org.wymix.framework.test.dbutil.dataset.xlsx.XlsxDataSet;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.excel.XlsDataSet;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractOperation {

    protected IDatabaseConnection connection;

    protected String fileName;

    public AbstractOperation(IDatabaseConnection connection, String fileName) {
        this.connection = connection;
        this.fileName = fileName;
    }

    public abstract void execute() throws Exception;

    IDataSet getDataSet() throws Exception {
        if (StringUtils.endsWithIgnoreCase(fileName, "xls")) {
            return new XlsDataSet(ResourceUtils.getFile(fileName));
        }
        if (StringUtils.endsWithIgnoreCase(fileName, "xlsx")) {
            return new XlsxDataSet(ResourceUtils.getFile(fileName));
        }
        throw new IllegalAccessException("not support file type");
    }

}
