package ogs.model.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static ogs.model.ontology.AnnotationPropertyType.*;

public abstract class ResourceDTO {

    protected Vocabulary namespace;
    protected String id;
    protected List<AnnotationPropertyDTO> annotationProperties;

    public ResourceDTO(String id) {

        this(Vocabulary.local, id);
    }

    public ResourceDTO(Vocabulary namespace, String id) {

        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Argument for resource creation is null or blank.");

        String sanitizedId = id.strip().replaceAll("[\\s+|\\u00A0]", "_")
                .replaceAll("[^\\p{L}\\p{N}._-]", "_");

        if (sanitizedId.isEmpty()) {
            sanitizedId = UUID.randomUUID().toString();
        }
        this.id = sanitizedId;
        this.namespace = namespace;
    }

    public Vocabulary getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    public String getAnnotationProperty(AnnotationPropertyType type) {

        if (annotationProperties == null)
            return null;

        var annotationProperty = annotationProperties.stream()
                .filter(property -> Objects.equals(type, property.getType()))
                .findAny()
                .orElse(null);

        if (annotationProperty != null)
            return annotationProperty.getValue();
        else
            return null;
    }

    public String getAnnotationProperty(AnnotationPropertyType type, String langTag) {

        if (annotationProperties == null)
            return null;

        var annotationProperty = annotationProperties.stream()
                .filter(property -> Objects.equals(type, property.getType())
                        && Objects.equals(langTag, property.getLangTag()))
                .findAny()
                .orElse(null);

        if (annotationProperty != null)
            return annotationProperty.getValue();
        else
            return null;
    }

    public void addAnnotationProperty(AnnotationPropertyType type, String value) {

        addAnnotationProperty(type, value, null);
    }

    public void addAnnotationProperty(AnnotationPropertyType type, String value, String langTag) {

        if (getAnnotationProperty(type, langTag) != null)
            throw new RuntimeException("There already is an annotation property of such type" +
                    " with such langTag. This application is not designed for duplicate properties.");

        if (annotationProperties == null)
            annotationProperties = new ArrayList<>();

        if (value != null && !value.isBlank())
            annotationProperties.add(
                    new AnnotationPropertyDTO(
                            type,
                            value,
                            langTag
                    )
            );
    }

    public void removeAnnotationProperty(AnnotationPropertyDTO label) {

        if (annotationProperties == null
            || !annotationProperties.contains(label))
            return;

        annotationProperties.remove(label);
    }

    public String getLabel() {

        return getAnnotationProperty(label);
    }

    public String getLabel(String langTag) {

        return getAnnotationProperty(label, langTag);
    }

    public void addLabel(String labelValue) {

        addAnnotationProperty(label, labelValue);
    }

    public void addLabel(String labelValue, String langTag) {

        addAnnotationProperty(label, labelValue, langTag);
    }

    public String getComment(String langTag) {

        return getAnnotationProperty(comment, langTag);
    }

    public void addComment(String commentValue) {

        addAnnotationProperty(comment, commentValue);
    }

    public void addComment(String commentValue, String langTag) {

        addAnnotationProperty(comment, commentValue, langTag);
    }

    public List<AnnotationPropertyDTO> getAnnotationProperties() {
        return annotationProperties;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResourceDTO other = (ResourceDTO) obj;
        return namespace == other.namespace && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id);
    }
}
