package ogs.ui.views;

public enum View {
    Main("/ogs/ui/views/main-view.fxml"),
    BasicSettings("/ogs/ui/views/basic-settings-view.fxml"),
    KeywordSettings("/ogs/ui/views/keyword-settings-view.fxml"),
    AnalysisReport("/ogs/ui/views/analysis-report-view.fxml");

    private final String filePath;

    View(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
