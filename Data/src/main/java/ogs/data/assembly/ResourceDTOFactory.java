package ogs.data.assembly;

import ogs.model.ontology.ResourceDTO;

class ResourceDTOFactory<T extends ResourceDTO> {
    private final Class<T> type;

    public ResourceDTOFactory(Class<T> type) {
        this.type = type;
    }

    public T create(String id) {
        try {
            return type.getConstructor(String.class).newInstance(id);
        } catch (Exception e) {
            throw new RuntimeException("Could not create instance of "
                    + type.getName(), e);
        }
    }
}
