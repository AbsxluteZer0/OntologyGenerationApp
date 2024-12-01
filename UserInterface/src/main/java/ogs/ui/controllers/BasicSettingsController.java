package ogs.ui.controllers;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.TextField;
import ogs.model.core.RegExPatterns;
import ogs.ui.model.ConfigurationProperty;

public class BasicSettingsController {

    private final ConfigurationProperty configProperty;

    @FXML private TextField sourceFilePathField;
    @FXML private TextField localizationDictionaryFilePathField;
    @FXML private TextField outputDirectoryField;
    @FXML private TextField ontologyFileNameField;
    @FXML private TextField sheetIdField;
    @FXML private TextField hierarchyLevelIndicatorField;
    @FXML private TextField dataCleansingRegex;

    public BasicSettingsController(ConfigurationProperty configProperty) {
        this.configProperty = configProperty;
    }

    @FXML
    public void initialize() {

        sourceFilePathField.textProperty().bindBidirectional(configProperty.getSourceFilePathProperty());
        localizationDictionaryFilePathField.textProperty().bindBidirectional(configProperty.getLocalizationDictionaryFilePathProperty());
        outputDirectoryField.textProperty().bindBidirectional(configProperty.getOutputDirectoryProperty());
        ontologyFileNameField.textProperty().bindBidirectional(configProperty.getOntologyFileNameProperty());
        ontologyFileNameField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // focus lost
                String text = ontologyFileNameField.getText();
                if (text != null) {
                    ontologyFileNameField.setText(text.replaceAll(RegExPatterns.PATH_VALIDATION, ""));
                }
            }
        });
        sheetIdField.textProperty().bindBidirectional(configProperty.getSheetIdStringProperty());
        sheetIdField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // focus lost
                try {
                    int value = Integer.parseInt(sheetIdField.getText());
                    configProperty.getSheetIdProperty().set(value);
                } catch (NumberFormatException e) {
                    sheetIdField.setText("-1");
                }
            }
        });
        hierarchyLevelIndicatorField.textProperty().bindBidirectional(configProperty.getClassHierarchySubclassIndicatorProperty());
        dataCleansingRegex.textProperty().bindBidirectional(configProperty.getDataCleansingRegexProperty());

        configureTextFieldToShowEnd(sourceFilePathField);
        configureTextFieldToShowEnd(localizationDictionaryFilePathField);
        configureTextFieldToShowEnd(outputDirectoryField);
    }


    private void configureTextFieldToShowEnd(TextField textField) {

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String text = textField.getText();
                if (text != null) {
                    textField.positionCaret(text.length());
                }
            }
        });
    }

    @FXML
    private void openSourceFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Excel Files (*.xls, *.xlsx, *.xlsm)", "*.xls", "*.xlsx", "*.xlsm"));
        fileChooser.setTitle("Select Source File");
        var file = fileChooser.showOpenDialog(null);
        if (file != null) {
            sourceFilePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void openLocalizationDictionaryFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Excel Files (*.xls, *.xlsx, *.xlsm)", "*.xls", "*.xlsx", "*.xlsm"));
        fileChooser.setTitle("Select Localization Dictionary File");
        var file = fileChooser.showOpenDialog(null);
        if (file != null) {
            localizationDictionaryFilePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void openOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        var directory = directoryChooser.showDialog(null);
        if (directory != null) {
            outputDirectoryField.setText(directory.getAbsolutePath());
        }
    }
}