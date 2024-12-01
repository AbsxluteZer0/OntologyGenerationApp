package ogs.model.ontology;

import java.util.Objects;

public class AnnotationPropertyDTO extends HierarchicalDTO<AnnotationPropertyDTO> {

    private final AnnotationPropertyType type;
    private final String value;
    private final String langTag;

    public AnnotationPropertyDTO(AnnotationPropertyType type, String value) {
        this(type, value, null);
    }

    public AnnotationPropertyDTO(AnnotationPropertyType type, String value, String langTag) {

        super(AnnotationPropertyTypeToVocabularyMapper.Map(type), type.toString());

        if (value == null || value.isBlank())
            throw new IllegalArgumentException("The property value must not be null or blank.");

        this.type = type;
        this.value = value.strip();
        this.langTag = langTag == null ? null : langTag.strip();
    }

    public AnnotationPropertyType getType() {
        return type;
    }
    public String getValue() {
        return value;
    }
    public String getLangTag() {
        return langTag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id, type, value, langTag);
    }
}
