package com.travelsky.base.test.dbutil.dataset.xlsx;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dbunit.dataset.excel.XlsDataSetWriter;

public class XlsxDataSetWriter extends XlsDataSetWriter {
    @Override
    protected Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

}
