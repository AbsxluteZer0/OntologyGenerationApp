package ogs.ui.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import ogs.model.core.KeywordMatchingOption;
import ogs.model.core.TableAnalysisConcept;
import ogs.ui.model.ConfigurationProperty;
import ogs.ui.model.KeywordEntry;

import java.util.*;

public class KeywordSettingsController {

    private final ConfigurationProperty configProperty;

    @FXML private TableView<KeywordEntry> keywordsTable;
    @FXML private TableColumn<KeywordEntry, String> keywordColumn;
    @FXML private TableColumn<KeywordEntry, String> conceptColumn;
    @FXML private TableColumn<KeywordEntry, Boolean> completeMatchColumn;
    @FXML private TableColumn<KeywordEntry, Boolean> caseSensitiveColumn;
    private boolean isTableUpdateInProgress;

    public KeywordSettingsController(ConfigurationProperty configProperty) {

        this.configProperty = configProperty;
    }

    public void initialize() {

        ObservableList<String> conceptsList = FXCollections.observableArrayList(
                Arrays.stream(TableAnalysisConcept.values())
                        .map(TableAnalysisConcept::getDisplayString)
                        .toList());

        keywordColumn.setCellValueFactory(cellData -> cellData.getValue().getKeywordProperty());
        keywordColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        EventHandler<TableColumn.CellEditEvent<KeywordEntry, String>> cellEditCancelHandler = event -> {
            ObservableList<KeywordEntry> items = keywordsTable.getItems();
            String newValue = event.getNewValue();
            KeywordEntry rowEntry = event.getRowValue();
            if (rowEntry == null || rowEntry.isNew()) return;

            if (newValue == null || newValue.isBlank()) {
                String oldValue = event.getOldValue();
                if (oldValue != null && !oldValue.isBlank()) {
                    rowEntry.getKeywordProperty().set(oldValue);
                } else {
                    if (rowEntry.getKeyword() == null
                            || rowEntry.getKeyword().isBlank()) {
                        items.remove(rowEntry);
                    }
                }
            }
        };
        EventHandler<TableColumn.CellEditEvent<KeywordEntry, String>> cellEditCommitHandler = event -> {
            ObservableList<KeywordEntry> items = keywordsTable.getItems();
            String newValue = event.getNewValue();
            KeywordEntry rowEntry = event.getRowValue();
            rowEntry.setNew(false);

            if (newValue == null || newValue.isBlank()) {
                String oldValue = event.getOldValue();
                if (oldValue != null && !oldValue.isBlank()) {
                    rowEntry.getKeywordProperty().set(oldValue);
                } else {
                    items.remove(rowEntry);
                }
            } else {
                rowEntry.getKeywordProperty().set(newValue);
            }
        };
        keywordColumn.setOnEditCommit(cellEditCommitHandler);
        keywordColumn.setOnEditCancel(cellEditCancelHandler);

        conceptColumn.setCellValueFactory(cellData -> cellData.getValue().getConceptStringProperty());
        conceptColumn.setCellFactory(ComboBoxTableCell.forTableColumn(conceptsList));

        completeMatchColumn.setCellValueFactory(cellData -> cellData.getValue().getCompleteMatchProperty());
        completeMatchColumn.setCellFactory(CheckBoxTableCell.forTableColumn(completeMatchColumn));

        caseSensitiveColumn.setCellValueFactory(cellData -> cellData.getValue().getCaseSensitiveProperty());
        caseSensitiveColumn.setCellFactory(CheckBoxTableCell.forTableColumn(caseSensitiveColumn));

        keywordsTable.setItems(configProperty.getKeywordEntries());
        keywordsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        keywordsTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeAllSelectedEntries();
                event.consume();
            }
        });
        keywordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        keywordsTable.getFocusModel().focusedCellProperty().addListener((obs, oldFocus, newFocus) -> {
            if (newFocus.getRow() == -1) {
                Platform.runLater(() -> {
                    if (!keywordsTable.getItems().isEmpty()) {
                        keywordsTable.getFocusModel().focus(0, keywordColumn);
                    }
                });
            }
        });

        addContextMenuToTable();
        addRemoveButtonColumn();
        adjustColumnWidths();
    }

    private void removeAllSelectedEntries() {
        ObservableList<KeywordEntry> selectedItems = keywordsTable.getSelectionModel().getSelectedItems();
        List<KeywordEntry> itemsToRemove = new ArrayList<>(selectedItems);
        keywordsTable.getItems().removeAll(itemsToRemove);
    }

    @FXML
    private void onAddKeyword() {

        if (isTableUpdateInProgress) return;

        isTableUpdateInProgress = true;
        ObservableList<KeywordEntry> items = keywordsTable.getItems();

        if (!items.isEmpty()) {
            KeywordEntry last = items.getLast();
            if (last.getKeyword() == null || last.getKeyword().isBlank()) {
                keywordsTable.scrollTo(items.size() - 1);
                keywordsTable.getSelectionModel().select(items.size() - 1);
                keywordsTable.getFocusModel().focus(items.size() - 1, keywordColumn);
                keywordsTable.edit(items.size() - 1, keywordColumn);
                isTableUpdateInProgress = false;
                return;
            }
        }

        KeywordEntry tempEntry = new KeywordEntry(
                "",
                TableAnalysisConcept.ClassColumn,
                EnumSet.noneOf(KeywordMatchingOption.class)
        );

        items.add(tempEntry);
        int newRowIndex = items.indexOf(tempEntry);

        Platform.runLater(() -> {
            keywordsTable.scrollTo(newRowIndex);
            keywordsTable.getSelectionModel().select(newRowIndex);
            keywordsTable.getFocusModel().focus(newRowIndex, keywordColumn);
            keywordsTable.edit(newRowIndex, keywordColumn);
            isTableUpdateInProgress = false;
        });
    }

    private void addContextMenuToTable() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem removeItem = new MenuItem("Remove selected");
        removeItem.setOnAction(e -> removeAllSelectedEntries());
        contextMenu.getItems().add(removeItem);
        keywordsTable.setContextMenu(contextMenu);
    }

    private void addRemoveButtonColumn() {

        TableColumn<KeywordEntry, Void> removeColumn = new TableColumn<>("Remove");

        removeColumn.setCellFactory(col -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");
            private final StackPane stackPane = new StackPane(removeButton);

            {
                removeButton.setOnAction(event -> {
                    KeywordEntry entry = getTableView().getItems().get(getIndex());
                    configProperty.getKeywordEntries().remove(entry);
                });

                stackPane.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(stackPane);
                }
            }
        });

        keywordsTable.getColumns().add(removeColumn);
    }

    private void adjustColumnWidths() {

        keywordsTable.getColumns().forEach(column -> {
            String headerText = column.getText();
            if (headerText != null && !headerText.isEmpty()) {
                Text headerNode = new Text(headerText);
                double headerWidth = headerNode.getBoundsInLocal().getWidth() + 10;

                column.setPrefWidth(headerWidth);
            }
        });

        double maxCellWidth = keywordsTable.getItems().stream()
                .map(item -> item.getConceptStringProperty().get())
                .filter(Objects::nonNull)
                .mapToDouble(text -> {
                    Text textNode = new Text(text);
                    return textNode.getBoundsInLocal().getWidth();
                })
                .max()
                .orElse(0);

        if (maxCellWidth + 10 > conceptColumn.getWidth())
            conceptColumn.setPrefWidth(maxCellWidth + 10);
    }

    public TableView<KeywordEntry> getKeywordsTable() {
        return keywordsTable;
    }
}