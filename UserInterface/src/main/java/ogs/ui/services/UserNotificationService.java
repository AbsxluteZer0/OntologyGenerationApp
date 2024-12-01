package ogs.ui.services;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserNotificationService {

    private final Labeled messageHost;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public UserNotificationService(Labeled messageHost) {
        this.messageHost = messageHost;
    }

    public void info(String message) {

        System.out.println(message + " (by UserNotificationService's info())");
        Platform.runLater(() -> messageHost.setText(message));
        scheduler.schedule(() -> Platform.runLater(() -> messageHost.setText("")), 10, TimeUnit.SECONDS);
    }

    public void warning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText("Warning!");
        alert.setContentText(message);

        alert.showAndWait();
    }

    public void warning(String headerText, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText(headerText);
        alert.setContentText(ex.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace is:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public void error(Exception ex) {

        error("An exception has occurred", ex);
    }

    public void error(String headerText, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText(headerText);
        alert.setContentText(ex.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace is:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }
}
