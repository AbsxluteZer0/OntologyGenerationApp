package ogs.data.analysis;

import ogs.data.core.SheetDataExtractor;
import ogs.data.core.WorkbookDataExtractor;
import ogs.model.core.AnalysisKeywordDictionary;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;

public class WorkbookAnalyzer {

    private final WorkbookDataExtractor workbookDataExtractor;

    private String creator;
    private String lastModifiedByUser;
    private String description;
    private String keywords;
    private String langTag;
    private final List<SheetAnalyzer> sheetAnalyzers;

    public WorkbookAnalyzer(WorkbookDataExtractor workbookDataExtractor) {
        this.workbookDataExtractor = workbookDataExtractor;
        sheetAnalyzers = new ArrayList<>();
    }

    public void analyzeMetadata() {

        if (workbookDataExtractor.tryExtractMetadata()) {
            creator = workbookDataExtractor.getCreator();
            lastModifiedByUser = workbookDataExtractor.getLastModifiedByUser();
            description = workbookDataExtractor.getDescription();
            keywords = workbookDataExtractor.getKeywords();
            langTag = workbookDataExtractor.getLangTag();
        }
    }

    public void analyzeSheet(AnalysisKeywordDictionary analysisKeywords, String hierarchyLevelIndicator, int sheetId) {

        Sheet sheet = workbookDataExtractor.getSheetAtOrActive1Based(sheetId);

        var sheetDataExtractor = new SheetDataExtractor(sheet);
        var sheetAnalyzer = new SheetAnalyzer(sheetDataExtractor, analysisKeywords, hierarchyLevelIndicator);

        sheetAnalyzer.initialize();
        sheetAnalyzer.analyze();

        sheetAnalyzers.add(sheetAnalyzer);
    }

    public void analyzeAllSheets(AnalysisKeywordDictionary analysisKeywords, String hierarchyLevelIndicator) {

        int sheetCount = workbookDataExtractor.getNumberOfSheets();

        for (int sheetId = 1; sheetId <= sheetCount; sheetId++) {
            analyzeSheet(analysisKeywords, hierarchyLevelIndicator, sheetId);
        }
    }

    public List<SheetAnalyzer> getSheetAnalyzers() {
        return sheetAnalyzers;
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
