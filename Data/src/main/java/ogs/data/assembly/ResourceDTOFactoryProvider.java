package ogs.data.assembly;

import ogs.model.core.TableAnalysisConcept;
import ogs.model.ontology.*;

class ResourceDTOFactoryProvider {

    private final TableAnalysisConcept concept;

    public ResourceDTOFactoryProvider(TableAnalysisConcept concept) {

        this.concept = concept;
    }

    public ResourceDTOFactory<? extends ResourceDTO> getInstance() {
        return getInstance(concept);
    }

    public static ResourceDTOFactory<? extends ResourceDTO> getInstance(TableAnalysisConcept concept) {

        return switch (concept) {
            case IdentifierColumn -> new ResourceDTOFactory<>(IndividualDTO.class);
            case ClassColumn, PredefinedClassHierarchyColumn,
                    ClassBindingSourceColumn, ClassBindingTargetColumn -> new ResourceDTOFactory<>(ClassDTO.class);
            case DataPropertyColumn,
                    PredefinedDataPropertyHierarchyColumn -> new ResourceDTOFactory<>(DataPropertyDTO.class);
            case ObjectPropertyColumn,
                    PredefinedObjectPropertyHierarchyColumn -> new ResourceDTOFactory<>(ObjectPropertyDTO.class);
            case PredefinedAnnotationPropertyHierarchyColumn -> new ResourceDTOFactory<>(AnnotationPropertyDTO.class);
            default -> throw new RuntimeException("Not supported concept " + concept);
        };
    }
}
