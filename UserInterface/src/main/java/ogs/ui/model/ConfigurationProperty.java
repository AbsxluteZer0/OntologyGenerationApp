package ogs.ui.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ogs.model.core.AnalysisKeywordDictionary;
import ogs.model.core.Configuration;
import ogs.model.core.KeywordMatchingOption;

import java.util.EnumSet;

public class ConfigurationProperty {

    private final Configuration config;

    private final StringProperty sourceFilePath;
    private final StringProperty localizationDictionaryFilePath;
    private final StringProperty outputDirectory;
    private final StringProperty ontologyFileName;
    private final IntegerProperty sheetId;
    private final StringProperty sheetIdString;
    private final StringProperty classHierarchySubclassIndicator;
    private final StringProperty dataCleansingRegex;
    private final ObservableList<KeywordEntry> keywordEntries;

    public ConfigurationProperty(Configuration config) {

        this.config = config;

        this.sourceFilePath = new SimpleStringProperty(config.getSourceFilePath());
        this.localizationDictionaryFilePath = new SimpleStringProperty(config.getLocalizationDictionaryFilePath());
        this.outputDirectory = new SimpleStringProperty(config.getOutputDirectory());
        this.ontologyFileName = new SimpleStringProperty(config.getOntologyFileName());
        this.sheetId = new SimpleIntegerProperty(config.getSheetId());
        this.sheetIdString = new SimpleStringProperty(String.valueOf(config.getSheetId()));
        this.classHierarchySubclassIndicator = new SimpleStringProperty(config.getHierarchyLevelIndicator());
        this.dataCleansingRegex = new SimpleStringProperty(config.getDataCleansingRegex());

        this.sheetId.addListener((obs, oldVal, newVal) -> sheetIdString.set(String.valueOf(newVal.intValue())));
        this.sheetIdString.addListener((obs, oldVal, newVal) -> {
            try {
                sheetId.set(Integer.parseInt(newVal));
            } catch (NumberFormatException e) {
                // or to explicitly indicate
            }
        });

        this.keywordEntries = FXCollections.observableArrayList();

        AnalysisKeywordDictionary keywords = config.getKeywords();
        if (keywords != null) {
            keywords.getKeywordsMap().forEach((keyword, concept) -> {
                EnumSet<KeywordMatchingOption> options = keywords.getOptionsMap()
                        .getOrDefault(keyword, EnumSet.noneOf(KeywordMatchingOption.class));
                this.keywordEntries.add(new KeywordEntry(keyword, concept, options));
            });
        }

        this.keywordEntries.forEach(this::attachListenersToEntry);
        this.keywordEntries.addListener((ListChangeListener<KeywordEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::addOrUpdateKeywordInConfig);
                    change.getAddedSubList().forEach(this::attachListenersToEntry);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::removeKeywordFromConfig);
                }
            }
        });

        bindPropertiesToConfiguration();
    }

    private void bindPropertiesToConfiguration() {

        sourceFilePath.addListener((obs, oldVal, newVal) -> config.setSourceFilePath(newVal));
        localizationDictionaryFilePath.addListener((obs, oldVal, newVal) -> config.setLocalizationDictionaryFilePath(newVal));
        outputDirectory.addListener((obs, oldVal, newVal) -> config.setOutputDirectory(newVal));
        ontologyFileName.addListener((obs, oldVal, newVal) -> config.setOntologyFileName(newVal));
        sheetId.addListener((obs, oldVal, newVal) -> config.setSheetId(newVal.intValue()));
        classHierarchySubclassIndicator.addListener((obs, oldVal, newVal) -> config.setHierarchyLevelIndicator(newVal));
        dataCleansingRegex.addListener(((obs, oldVal, newVal) -> config.setDataCleansingRegex(newVal)));
    }

    private void attachListenersToEntry(KeywordEntry entry) {

        entry.getKeywordProperty().addListener((obs, oldVal, newVal)
                -> updateKeywordValueInConfig(oldVal, entry));

        entry.getConceptProperty().addListener((obs, oldVal, newVal)
                -> addOrUpdateKeywordInConfig(entry));

        entry.getCompleteMatchProperty().addListener((obs, oldVal, newVal)
                -> addOrUpdateKeywordInConfig(entry));

        entry.getCaseSensitiveProperty().addListener((obs, oldVal, newVal)
                -> addOrUpdateKeywordInConfig(entry));
    }

    private void addOrUpdateKeywordInConfig(KeywordEntry entry) {

        config.getKeywords().put(entry.getKeyword(), entry.getConcept(), entry.getOptions());
    }

    private void removeKeywordFromConfig(KeywordEntry entry) {
        config.getKeywords().remove(entry.getKeyword());
    }

    private void updateKeywordValueInConfig(String oldKeywordValue, KeywordEntry entry) {

        AnalysisKeywordDictionary keywords = config.getKeywords();
        keywords.remove(oldKeywordValue);
        keywords.put(entry.getKeyword(), entry.getConcept(), entry.getOptions());
    }

    public StringProperty getSourceFilePathProperty() { return sourceFilePath; }
    public StringProperty getLocalizationDictionaryFilePathProperty() { return localizationDictionaryFilePath; }
    public StringProperty getOutputDirectoryProperty() { return outputDirectory; }
    public StringProperty getOntologyFileNameProperty() { return ontologyFileName; }
    public IntegerProperty getSheetIdProperty() {
        return sheetId;
    }
    public StringProperty getSheetIdStringProperty() { return sheetIdString; }
    public StringProperty getClassHierarchySubclassIndicatorProperty() { return classHierarchySubclassIndicator; }
    public ObservableList<KeywordEntry> getKeywordEntries() {
        return keywordEntries;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public Property<String> getDataCleansingRegexProperty() {
        return dataCleansingRegex;
    }
}
