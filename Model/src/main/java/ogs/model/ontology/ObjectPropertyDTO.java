package ogs.model.ontology;

import org.apache.commons.lang3.StringUtils;

public class ObjectPropertyDTO extends HierarchicalDTO<ObjectPropertyDTO> {

    private final IndividualDTO object;

    public ObjectPropertyDTO(String id) {
        this(id, null);
    }

    public ObjectPropertyDTO(String id, IndividualDTO object) {
        this(Vocabulary.local, id, object);
    }

    public ObjectPropertyDTO(Vocabulary namespace, String id) {
        this(namespace, id, null);
    }

    public ObjectPropertyDTO(Vocabulary namespace, String id, IndividualDTO object) {
        super(namespace, StringUtils.uncapitalize(id));
        this.object = object;
    }

    public IndividualDTO getObject() {
        return object;
    }
}
