package ogs.data.analysis;

import ogs.data.core.WorkbookDataExtractor;
import ogs.data.core.WorkbookManager;
import ogs.model.core.Configuration;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;

public class OntologyDataProvider {

    private final Configuration config;
    private WorkbookManager workbookManager;
    private WorkbookAnalyzer workbookAnalyzer;

    public OntologyDataProvider(Configuration config) {
        this.config = config;
    }

    public void initialize() throws IOException, InvalidFormatException {
        workbookManager = new WorkbookManager(config.getSourceFilePath());
        WorkbookDataExtractor workbookDataExtractor = new WorkbookDataExtractor(workbookManager.getWorkbook());
        workbookAnalyzer = new WorkbookAnalyzer(workbookDataExtractor);
    }

    public void extractData() {

        if (workbookAnalyzer == null)
            throw new IllegalStateException("Call initialize() first.");

        workbookAnalyzer.analyzeMetadata();

        var sheetId = config.getSheetId();
        if (sheetId == -411) { // Process all sheets
            workbookAnalyzer.analyzeAllSheets(config.getKeywords(), config.getHierarchyLevelIndicator());
        } else { // Process the specified sheet or active sheet by default
            workbookAnalyzer.analyzeSheet(config.getKeywords(), config.getHierarchyLevelIndicator(), sheetId);
        }
    }

    public void close() throws IOException {
        if (workbookManager != null) workbookManager.close();
        workbookManager = null;
    }

    public WorkbookAnalyzer getWorkbookAnalyzer() {
        return workbookAnalyzer;
    }
}