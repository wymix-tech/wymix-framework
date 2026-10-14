package org.wymix.framework.test.dbutil.dataset.xlsx;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.dbunit.dataset.AbstractDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableIterator;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.OrderedTableNameMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XlsxDataSet extends AbstractDataSet {

    private static final Logger logger = LoggerFactory.getLogger(XlsxDataSet.class);

    private final OrderedTableNameMap _tables;

    public XlsxDataSet(File file) throws IOException, DataSetException {
        this(new FileInputStream(file));
    }

    public XlsxDataSet(InputStream in) throws IOException, DataSetException {
        _tables = super.createTableNameMap();

        Workbook workbook;
        try {
            workbook = XSSFWorkbookFactory.create(in);
        } catch (EncryptedDocumentException e) {
            throw new IOException(e);
        }
        int sheetCount = workbook.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            ITable table = new XlsxTable(workbook.getSheetName(i),
                    workbook.getSheetAt(i));
            _tables.add(table.getTableMetaData().getTableName(), table);
        }
    }

    public static void write(IDataSet dataSet, OutputStream out)
            throws IOException, DataSetException {
        logger.debug("write(dataSet={}, out={}) - start", dataSet, out);

        new XlsxDataSetWriter().write(dataSet, out);
    }

    @Override
    protected ITableIterator createIterator(boolean reversed) throws DataSetException {
        if (logger.isDebugEnabled())
            logger.debug("createIterator(reversed={}) - start", String.valueOf(reversed));

        ITable[] tables = (ITable[]) _tables.orderedValues().toArray(new ITable[0]);
        return new DefaultTableIterator(tables, reversed);
    }

}
