package ogs.model.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {

    @JsonProperty("Source file path")
    private String sourceFilePath;
    @JsonProperty("Localization dictionary file path")
    private String localizationDictionaryFilePath;
    @JsonProperty("Output directory")
    private String outputDirectory;
    @JsonProperty("Ontology file name")
    private String ontologyFileName;
    @JsonProperty("Sheet ID")
    private int sheetId = -411; // -411 for all sheets, any invalid for the active sheet
    @JsonProperty("Keywords")
    private AnalysisKeywordDictionary keywords = new AnalysisKeywordDictionary();
    @JsonProperty("Hierarchy level indicator")
    private String hierarchyLevelIndicator;
    @JsonProperty("Data cleansing regex")
    private String dataCleansingRegex;

    // Default constructor (required for JSON deserialization)
    public Configuration() {}

    //region Getters and Setters
    public String getSourceFilePath() {
        return sourceFilePath;
    }

    /**
     * @param filePath .xls, .xlsx, .xlsm are supported.
     */
    public void setSourceFilePath(String filePath) {
        sourceFilePath = filePath;
    }

    public int getSheetId() {
        return sheetId;
    }

    /**
     *
     * @param sheetId
     * To process all sheets pass '-411';
     * any other invalid id defaults to active sheet.
     * E.g., for a document with 3 sheets:
     *      '-411' — all three,
     *      '0', '1' or '2' — the first, second or the third,
     *      any other possible value (e.g. -1) — the active sheet.
     */
    public void setSheetId(int sheetId) {
        this.sheetId = sheetId;
    }

    public AnalysisKeywordDictionary getKeywords() {
        return keywords;
    }

    public void setKeywords(AnalysisKeywordDictionary keywords) {
        this.keywords = keywords;
    }

    public String getHierarchyLevelIndicator() {
        return hierarchyLevelIndicator;
    }

    public void setHierarchyLevelIndicator(String hierarchyLevelIndicator) {
        this.hierarchyLevelIndicator = hierarchyLevelIndicator;
    }

    public String getLocalizationDictionaryFilePath() {
        return localizationDictionaryFilePath;
    }

    public void setLocalizationDictionaryFilePath(String localizationDictionaryFilePath) {
        this.localizationDictionaryFilePath = localizationDictionaryFilePath;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOntologyFileName() {
        return ontologyFileName;
    }

    public void setOntologyFileName(String ontologyFileName) {
        this.ontologyFileName = ontologyFileName;
    }

    public String getDataCleansingRegex() {
        return dataCleansingRegex;
    }

    public void setDataCleansingRegex(String dataCleansingRegex) {
        this.dataCleansingRegex = dataCleansingRegex;
    }
    //endregion
}
