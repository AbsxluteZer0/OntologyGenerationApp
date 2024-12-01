package ogs.data.core;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.util.Arrays;

public class LocalizationDictionaryProvider {
    private final WorkbookManager workbookManager;
    private final SheetDataExtractor sheetDataExtractor;

    public LocalizationDictionaryProvider(String dictionaryFilePath) throws IOException, InvalidFormatException {
        workbookManager = new WorkbookManager(dictionaryFilePath);
        sheetDataExtractor = new SheetDataExtractor(
                workbookManager.getWorkbook().getSheetAt(
                        workbookManager.getWorkbook().getActiveSheetIndex())
        );
    }

    public String[][] getDictionary() {
        return Arrays.stream(sheetDataExtractor.extractValues())
                .map(row -> Arrays.stream(row)
                        .map(CellValue::toString)
                        .toArray(String[]::new))
                .toArray(String[][]::new);
    }

    public void close() throws IOException {
        workbookManager.close();
    }
}
