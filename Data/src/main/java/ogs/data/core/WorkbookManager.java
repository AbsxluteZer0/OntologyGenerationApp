package ogs.data.core;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;

public class WorkbookManager implements AutoCloseable {
    private Workbook workbook;
    private final ExcelFileManager fileManager = new ExcelFileManager();

    public WorkbookManager(String filePath)
            throws IOException, InvalidFormatException {
        this.workbook = fileManager.read(filePath);
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    @Override
    public void close() throws IOException {
        if (workbook != null) workbook.close();
        fileManager.close();
        workbook = null;
    }
}
