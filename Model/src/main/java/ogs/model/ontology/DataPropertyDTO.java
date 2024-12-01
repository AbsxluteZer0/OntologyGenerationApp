package ogs.model.ontology;

import org.apache.commons.lang3.StringUtils;

public class DataPropertyDTO extends HierarchicalDTO<DataPropertyDTO> {

    private final Class<?> dataType;
    private final Object value;

    public DataPropertyDTO(String id) {
        this(id, null, null);
    }

    public DataPropertyDTO(Vocabulary namespace, String id) {
        this(namespace, id, null, null);
    }

    public DataPropertyDTO(String id, Object value, Class<?> dataType) {
        this(Vocabulary.local, id, value, dataType);
    }

    public DataPropertyDTO(Vocabulary namespace, String id, Object value, Class<?> dataType) {
        super(namespace, StringUtils.uncapitalize(id));
        this.value = value;
        this.dataType = dataType;
    }

    public Class<?> getType() {
        return dataType;
    }

    public Object getValue() {
        return value;
    }
}
