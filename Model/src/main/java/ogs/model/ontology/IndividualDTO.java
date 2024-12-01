package ogs.model.ontology;

import java.util.ArrayList;
import java.util.List;

public class IndividualDTO extends ResourceDTO {

    private List<ClassDTO> types;
    private List<DataPropertyDTO> dataProperties;
    private List<ObjectPropertyDTO> objectProperties;

    public IndividualDTO(String id) {
        super(id);
    }

    public IndividualDTO(String id, String displayName) {
        this(id);
        addLabel(displayName);
    }

    public void addType(ClassDTO type) {

        if (types == null)
            types = new ArrayList<>();

        if (type == null)
            return;

        if (types.contains(type))
            return;

        types.add(type);
    }

    public void addAllTypes(List<ClassDTO> typesToAdd) {

        if (types == null)
            types = new ArrayList<>();

        if (typesToAdd == null)
            return;

        for (var typeToAdd : typesToAdd) {

            if (typeToAdd == null
                || types.contains(typeToAdd))
                continue;

            types.add(typeToAdd);
        }
    }

    public void addDataProperty(DataPropertyDTO property) {

        if (dataProperties == null)
            dataProperties = new ArrayList<>();

        if (property == null)
            return;

        dataProperties.add(property);
    }

    public void addObjectProperty(ObjectPropertyDTO property) {

        if (objectProperties == null)
            objectProperties = new ArrayList<>();

        if (property == null)
            return;

        objectProperties.add(property);
    }

    public List<ClassDTO> getTypes() {

        if (types == null)
            types = new ArrayList<>();

        return types;
    }

    public List<DataPropertyDTO> getDataProperties() {

        if (dataProperties == null)
            dataProperties = new ArrayList<>();

        return dataProperties;
    }

    public List<ObjectPropertyDTO> getObjectProperties() {

        if (objectProperties == null)
            objectProperties = new ArrayList<>();

        return objectProperties;
    }
}
