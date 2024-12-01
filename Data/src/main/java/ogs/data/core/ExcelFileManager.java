package ogs.data.core;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class ExcelFileManager {

    private FileInputStream fileInputStream;
    private OPCPackage opcPackage;

    public Workbook read(String filePath)
            throws IOException, InvalidFormatException  {

        if (filePath.endsWith(".xls")) {
            fileInputStream = new FileInputStream(filePath);
            return new HSSFWorkbook(fileInputStream);
        } else { // .xlsx/.xlsm files are expected here
            opcPackage = OPCPackage.open(filePath);
            return new XSSFWorkbook(opcPackage);
        }
    }

    public void close() throws IOException {
        if (fileInputStream != null) fileInputStream.close();
        if (opcPackage != null) opcPackage.close();
    }
}
