package ogs.ui.model;

import javafx.beans.property.*;
import ogs.model.core.KeywordMatchingOption;
import ogs.model.core.TableAnalysisConcept;

import java.util.EnumSet;

public class KeywordEntry {

    private final StringProperty keywordProperty;
    private final ObjectProperty<TableAnalysisConcept> conceptProperty;
    private final StringProperty conceptStringProperty;
    private final BooleanProperty completeMatchProperty;
    private final BooleanProperty caseSensitiveProperty;
    private final BooleanProperty isNew;

    public KeywordEntry(String keyword, TableAnalysisConcept concept, EnumSet<KeywordMatchingOption> options) {
        this.keywordProperty = new SimpleStringProperty(keyword);
        this.conceptProperty = new SimpleObjectProperty<>(concept);
        this.conceptStringProperty = new SimpleStringProperty(concept.getDisplayString());
        this.completeMatchProperty = new SimpleBooleanProperty(options.contains(KeywordMatchingOption.CompleteMatch));
        this.caseSensitiveProperty = new SimpleBooleanProperty(options.contains(KeywordMatchingOption.CaseSensitive));
        this.isNew = new SimpleBooleanProperty(true);

        conceptProperty.addListener((obs, oldVal, newVal) -> {
            conceptStringProperty.set(newVal.getDisplayString());
        });
        conceptStringProperty.addListener((obs, oldVal, newVal) -> {
            conceptProperty.set(TableAnalysisConcept.getConcept(newVal));
        });
    }

    // Getters and setters for TableView binding
    public StringProperty getKeywordProperty() { return keywordProperty; }
    public ObjectProperty<TableAnalysisConcept> getConceptProperty() { return conceptProperty; }
    public StringProperty getConceptStringProperty() {
        return conceptStringProperty;
    }
    public BooleanProperty getCompleteMatchProperty() { return completeMatchProperty; }
    public BooleanProperty getCaseSensitiveProperty() { return caseSensitiveProperty; }

    public String getKeyword() { return keywordProperty.get(); }
    public TableAnalysisConcept getConcept() { return conceptProperty.get(); }
    public EnumSet<KeywordMatchingOption> getOptions() {
        EnumSet<KeywordMatchingOption> options = EnumSet.noneOf(KeywordMatchingOption.class);
        if (completeMatchProperty.get()) options.add(KeywordMatchingOption.CompleteMatch);
        if (caseSensitiveProperty.get()) options.add(KeywordMatchingOption.CaseSensitive);
        return options;
    }
    public boolean isNew() {
        return isNew.get();
    }
    public void setNew(boolean isNew) {
        this.isNew.set(isNew);
    }
}
