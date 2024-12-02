package ogs.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ogs.data.analysis.OntologyDataProvider;
import ogs.data.assembly.OntologyDTOFactory;
import ogs.data.core.LocalizationDictionaryProvider;
import ogs.localization.OntologyLabelLocalizationService;
import ogs.model.core.Configuration;
import ogs.model.core.RegExPatterns;
import ogs.model.ontology.ClassDTO;
import ogs.model.ontology.IndividualDTO;
import ogs.model.ontology.ResourceDTO;
import ogs.ontology.JenaOntologyManager;
import ogs.ui.services.UserNotificationService;
import ogs.ui.views.View;
import ogs.ui.model.ConfigurationProperty;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.*;

public class MainController {

    private final String configFilePath;
    private final String defaultConfigFilePath;
    private ConfigurationProperty configProperty;
    private UserNotificationService notificationService;

    // Cached views and controllers
    private Pane basicView;
    private BasicSettingsController basicController;
    private Pane keywordsView;
    private KeywordSettingsController keywordsController;

    @FXML public Pane currentView;
    @FXML public Label messageLabel;
    @FXML public Button saveAndRunButton;

    public static Object createInstance(String configFilePath, String defaultConfigFilePath) {
        return new MainController(configFilePath, defaultConfigFilePath);
    }

    public MainController(String configFilePath, String defaultConfigFilePath) {
        this.configFilePath = configFilePath;
        this.defaultConfigFilePath = defaultConfigFilePath;
    }

    public void initialize() {

        this.notificationService = new UserNotificationService(messageLabel);
        this.configProperty = new ConfigurationProperty(deserializeConfig(configFilePath));

        String saveAndRunButtonStyle
                = "-fx-font-size: 14px; -fx-padding: 8 20; -fx-background-color: #4CAF50; -fx-text-fill: white;";

        saveAndRunButton.setStyle(saveAndRunButtonStyle);
        saveAndRunButton.setOnMouseEntered(event -> saveAndRunButton.setStyle(
                saveAndRunButtonStyle + "-fx-background-color: #59cf5e;"));
        saveAndRunButton.setOnMouseExited(event -> saveAndRunButton.setStyle(saveAndRunButtonStyle));

        handleBasicTab();
    }

    @FXML
    public void handleBasicTab() {
        loadCachedView(View.BasicSettings.getFilePath(), () -> {
            basicController = new BasicSettingsController(configProperty);
            return basicController;
        });
    }

    @FXML
    public void handleKeywordsTab() {
        loadCachedView(View.KeywordSettings.getFilePath(), () -> {
            keywordsController = new KeywordSettingsController(configProperty);
            return keywordsController;
        });
    }

    private void loadCachedView(String fxmlPath, ControllerSupplier controllerSupplier) {

        if (fxmlPath.equals(View.BasicSettings.getFilePath()) && basicView == null) {
            basicView = loadView(fxmlPath, controllerSupplier.get());
        } else if (fxmlPath.equals(View.KeywordSettings.getFilePath()) && keywordsView == null) {
            keywordsView = loadView(fxmlPath, controllerSupplier.get());
        }
        currentView.getChildren().setAll(
                fxmlPath.equals(View.BasicSettings.getFilePath()) ? basicView : keywordsView);
        resizeWindow();
    }

    private Pane loadView(String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            notificationService.error("Failed to load a view!", e);
            return new Pane();
        }
    }

    @FXML
    private void handleSave() {
        serializeConfig();
    }

    @FXML
    private void handleRun() {
        runScript();
    }

    @FXML
    public void handleSaveAndRun() {
        serializeConfig();
        runScript();
    }

    private Configuration deserializeConfig(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        File configFile = new File(filePath);

        try {
            if (!configFile.exists()) {
                notificationService.info("Configuration file is missing. Loading default configuration...");
                if (!new File(defaultConfigFilePath).exists()) {
                    notificationService.info("Default configuration file is missing");
                    notificationService.warning("Default configuration file is missing. " +
                            "Make sure '" + defaultConfigFilePath +
                            "' file is present in the root directory to avoid this message in the future.");
                    return new Configuration();
                }
                return deserializeConfig(defaultConfigFilePath);
            } else if (configFile.length() == 0) {
                notificationService.info("Configuration file is empty");
                return new Configuration();
            }

            return objectMapper.readValue(configFile, Configuration.class);

        } catch (IOException e) {
            notificationService.warning(
                    "Failed to deserialize the configuration! The file will be overwritten on 'Save'.", e);
            return new Configuration();
        }
    }

    private void serializeConfig() {

        notificationService.info("Saving...");

        File configFile = new File(configFilePath);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            objectMapper.writeValue(configFile, configProperty.getConfiguration());
            notificationService.info("Saved successfully");
        } catch (IOException e) {
            notificationService.error("Failed to serialize the configuration!", e);
        }
    }

    private void runScript() {

        OntologyDataProvider dataProvider;
        LocalizationDictionaryProvider dictionaryProvider;
        JenaOntologyManager ontologyManager;
        Configuration config = configProperty.getConfiguration();
        String localizationDictionaryFilePath = config.getLocalizationDictionaryFilePath();
        RegExPatterns.setDataCleansingPattern(config.getDataCleansingRegex());

        try {
            dataProvider = new OntologyDataProvider(config);
            ontologyManager = new JenaOntologyManager(config);
            dataProvider.initialize();
        } catch (InvalidPathException e) {
            notificationService.warning("Invalid ontology output directory path!");
            return;
        } catch (IOException | InvalidFormatException e) {
            notificationService.error("Cannot open the source file!", e);
            return;
        }

        notificationService.info("Analyzing...");
        dataProvider.extractData();

        try {
            dataProvider.close();
        } catch (IOException ex) {
            notificationService.error(ex);
        }

        var workbookAnalyzer = dataProvider.getWorkbookAnalyzer();
        var sheetAnalyzers = workbookAnalyzer.getSheetAnalyzers();

        // ------------------------------
        // pause and show analysis results
        // ------------------------------
//        for (var sheetAnalyzer : sheetAnalyzers) {
//
//            LinkedList<ColumnAnalyzer> columns = new LinkedList<>(
//                    Arrays.stream(sheetAnalyzer.getColumnAnalyzers()).toList());
//            loadView(View.AnalysisReport.getFilePath(), new AnalysisReportController(columns));
//        }

        notificationService.info("Building the ontology...");

        List<ResourceDTO> ontologyResources = new ArrayList<>();
        Map<ClassDTO, ClassDTO> classBindingMap = new HashMap<>();

        for (var sheetAnalyzer : sheetAnalyzers) {

            OntologyDTOFactory resourceProvider = new OntologyDTOFactory(sheetAnalyzer);
            ontologyResources.addAll(resourceProvider.getAllResources());
            classBindingMap.putAll(resourceProvider.getClassBindingPairs());
        }

        if (localizationDictionaryFilePath != null && !localizationDictionaryFilePath.isBlank()) {
            try {
                dictionaryProvider = new LocalizationDictionaryProvider(localizationDictionaryFilePath);
                OntologyLabelLocalizationService localizationService
                        = new OntologyLabelLocalizationService(dictionaryProvider.getDictionary());
//                localizationService.attachAllPossibleLabelsTo(ontologyResources.stream()
//                        .filter(res -> res.getClass().equals(ClassDTO.class))
//                        .toList());
                localizationService.attachAllPossibleLabelsTo(ontologyResources);
            } catch (IOException | InvalidFormatException e) {
                notificationService.error("Localization has not been performed!" +
                        "Cannot open the localization dictionary file!", e);
            }
        }

        try {
            ontologyManager.initialize();
        } catch (RiotNotFoundException e) {
            notificationService.info("Ontology file not found and will be created");
        } catch (RiotException e) {
            notificationService.error("There is a problem reading specified ontology file." , e);
        } catch (IOException e) {
            notificationService.error(e);
        }

        notificationService.info("Saving the ontology file...");

        // may set OntologyMetadata object here populated with metadata from user or source file
        ontologyManager.setOntologyMetadata();

        ontologyManager.populateWith(ontologyResources);
        ontologyManager.createAssociations(ontologyResources.stream()
                .filter(res -> res instanceof IndividualDTO)
                .map(res -> (IndividualDTO) res)
                .toList());
        ontologyManager.bindClasses(classBindingMap);

        try {
            ontologyManager.save();
        } catch (IOException e) {
            notificationService.error(e);
        }

        notificationService.info("The ontology has been successfully created");
    }

    @FunctionalInterface
    private interface ControllerSupplier {
        Object get();
    }

    private void resizeWindow() {
        Platform.runLater(() -> {
            Stage stage = (Stage) currentView.getScene().getWindow();
            stage.sizeToScene();
        });
    }
}