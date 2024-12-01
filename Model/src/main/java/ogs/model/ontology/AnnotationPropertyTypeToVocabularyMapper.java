package ogs.model.ontology;

import static ogs.model.ontology.Vocabulary.*;

public class AnnotationPropertyTypeToVocabularyMapper {
    public static Vocabulary Map(AnnotationPropertyType type) {
        return switch (type) {
            case label, comment -> rdfs;
            default -> local;
        };
    }
}
