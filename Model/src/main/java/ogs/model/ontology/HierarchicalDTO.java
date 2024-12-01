package ogs.model.ontology;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class HierarchicalDTO<T extends HierarchicalDTO<T>>
        extends ResourceDTO {

    private List<T> ancestors;
    private List<T> descendants;

    public HierarchicalDTO(String id) {
        super(id);
    }

    public HierarchicalDTO(Vocabulary namespace, String id) {
        super(namespace, id);
    }

    public void addAncestor(T ancestor) {

        if (ancestors == null)
            ancestors = new ArrayList<>();

        if (ancestors.contains(ancestor))
            return;

        ancestor.addDescendant((T)this);
        ancestors.add(ancestor);
    }

    public void addDescendant(T descendant) {

        if (descendants == null)
            descendants = new ArrayList<>();

        if (descendants.contains(descendant))
            return;

        descendant.getAncestors().add((T)this);
        descendants.add(descendant);
    }

    public List<T> getAncestors() {

        if (ancestors == null)
            ancestors = new ArrayList<>();

        return ancestors;
    }

    public List<T> getDescendants() {

        if (descendants == null)
            descendants = new ArrayList<>();

        return descendants;
    }
}
