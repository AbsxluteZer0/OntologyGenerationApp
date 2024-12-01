package ogs.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import ogs.data.core.CellValue;
import ogs.data.analysis.ColumnAnalyzer;
import ogs.data.analysis.SheetAnalyzer;

import java.util.List;

public class AnalysisReportController {

    private final List<SheetAnalyzer> sheetAnalyzers;
    private int currentColumn = 0;
    private final int columnCount;

    @FXML
    private GridPane gridPane;
    @FXML
    private Label idIndicator;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;

    public AnalysisReportController(List<SheetAnalyzer> sheetAnalyzers) {
        if (sheetAnalyzers == null || sheetAnalyzers.isEmpty()) {
            throw new IllegalArgumentException("Sheet analyzers cannot be null or empty.");
        }
        this.sheetAnalyzers = sheetAnalyzers;
        this.columnCount = sheetAnalyzers.size();
    }

    @FXML
    private void initialize() {

        updateControlPanel();

        loadColumn(currentColumn);

        prevButton.setOnAction(event -> navigateToColumn(currentColumn - 1));
        nextButton.setOnAction(event -> navigateToColumn(currentColumn + 1));
    }

    private void navigateToColumn(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < columnCount) {
            currentColumn = columnIndex;
            loadColumn(currentColumn);
            updateControlPanel();
        }
    }

    private void loadColumn(int columnIndex) {

        gridPane.getChildren().clear();

        ColumnAnalyzer[] analyzers = sheetAnalyzers.getFirst().getColumnAnalyzers();

        Label header = new Label(analyzers[0].getHeader());
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-alignment: center;");
        gridPane.add(header, 1, 1);

        // Add row indices and elements
        CellValue[] elements = analyzers[0].getColumnData();
        for (int i = 0; i < elements.length; i++) {
            // Row index
            Label rowIndex = new Label(String.valueOf(i + 1));
            rowIndex.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
            gridPane.add(rowIndex, 0, i + 2);

            // Cell content
            Label cellContent = new Label(elements[i].toString());
            gridPane.add(cellContent, 1, i + 2);
        }

        // Add top column index (e.g., "A", "B", ...)
        char columnLetter = (char) ('A' + columnIndex);
        Label columnLabel = new Label(String.valueOf(columnLetter));
        columnLabel.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
        gridPane.add(columnLabel, 1, 0);
    }

    private void updateControlPanel() {
        idIndicator.setText((currentColumn + 1) + " of " + columnCount);
        prevButton.setDisable(currentColumn == 0);
        nextButton.setDisable(currentColumn == columnCount - 1);
    }
}

