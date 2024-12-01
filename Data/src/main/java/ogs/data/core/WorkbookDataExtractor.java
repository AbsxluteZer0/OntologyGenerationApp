package ogs.data.core;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WorkbookDataExtractor {

    Workbook workbook;

    private String title;
    private String creator;
    private String lastModifiedByUser;
    private String description;
    private String keywords;
    private String langTag;

    public WorkbookDataExtractor(Workbook workbook) {
        this.workbook = workbook;
    }

    public boolean tryExtractMetadata() {

        XSSFWorkbook xssfWorkbook;

        if (workbook instanceof SXSSFWorkbook sxssfWorkbook) {
            xssfWorkbook = sxssfWorkbook.getXSSFWorkbook();
        }
        else if (workbook instanceof XSSFWorkbook) {
            xssfWorkbook = (XSSFWorkbook) workbook;
        }
        else { // if no CoreProperties to look for
            return false;
        }

        var coreProperties = xssfWorkbook.getProperties().getCoreProperties();
        title = coreProperties.getTitle();
        creator = coreProperties.getCreator();
        lastModifiedByUser = coreProperties.getLastModifiedByUser();
        description = coreProperties.getDescription();
        keywords = coreProperties.getKeywords();
        langTag = coreProperties.getUnderlyingProperties().getLanguageProperty().orElse(null);

        return true;
    }

    public int getNumberOfSheets() {
        return workbook.getNumberOfSheets();
    }

    public Sheet getSheetAtOrActive1Based(int sheetId1Based) {
        return getSheetAtOrActive(sheetId1Based - 1);
    }

    public Sheet getSheetAtOrActive(int sheetId) {
        try {
            return workbook.getSheetAt(sheetId);
        }
        catch (IllegalArgumentException ex) {
            return workbook.getSheetAt(workbook.getActiveSheetIndex());
        }
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getLastModifiedByUser() {
        return lastModifiedByUser;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getLangTag() {
        return langTag;
    }
}
