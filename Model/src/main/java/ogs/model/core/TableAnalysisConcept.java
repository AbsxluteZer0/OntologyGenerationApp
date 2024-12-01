package ogs.model.core;

public enum TableAnalysisConcept {
    IdentifierColumn("Identifier column"),
    LabelColumn("Label column"),
    CommentColumn("Comment column"),
    ClassColumn("Class column"),
    DataPropertyColumn("Data property column"),
    ObjectPropertyColumn("Object property column"),
    PredefinedClassHierarchyColumn("Class hierarchy definition"),
    PredefinedDataPropertyHierarchyColumn("Data property hierarchy definition"),
    PredefinedObjectPropertyHierarchyColumn("Object property hierarchy definition"),
    PredefinedAnnotationPropertyHierarchyColumn("Annotation property hierarchy definition"),
    ClassBindingSourceColumn("Existing types of individuals"),
    ClassBindingTargetColumn("Associated types for individuals"),
    ColumnToIgnore("Column to ignore");

    private final String displayString;

    TableAnalysisConcept(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static TableAnalysisConcept getConcept(String displayString) {
        for (TableAnalysisConcept concept : TableAnalysisConcept.values()) {
            if (concept.displayString.equals(displayString))
                return concept;
        }
        throw new IllegalArgumentException(String.format(
                "No enum constant %s.%s",
                TableAnalysisConcept.class,
                displayString));
    }
}
