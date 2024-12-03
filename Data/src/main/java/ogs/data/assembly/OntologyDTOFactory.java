package ogs.data.assembly;

import ogs.data.core.CellValue;
import ogs.data.analysis.ColumnAnalyzer;
import ogs.data.analysis.SheetAnalyzer;
import ogs.model.core.TableAnalysisConcept;
import ogs.model.ontology.*;
import org.apache.poi.ss.usermodel.CellType;

import java.util.*;
import java.util.stream.Stream;

import static ogs.model.core.TableAnalysisConcept.*;

public class OntologyDTOFactory {

    private final SheetAnalyzer sheetAnalyzer;
    private final Map<TableAnalysisConcept, List<ColumnAnalyzer>> columnsToBuild;
    private final List<ResourceDTO> resources = new ArrayList<>();
    private final boolean thereAreIndividuals;

    public OntologyDTOFactory(SheetAnalyzer sheetAnalyzer) {

        this.sheetAnalyzer = sheetAnalyzer;
        this.columnsToBuild = HashMap.newHashMap(sheetAnalyzer.getColumnAnalyzers().length);

        for (var column : sheetAnalyzer.getColumnAnalyzers()) {
            TableAnalysisConcept concept = column.getDefinition();
            if (concept == ColumnToIgnore)
                continue;
            columnsToBuild.putIfAbsent(concept, new ArrayList<>());
            columnsToBuild.get(concept).add(column);
        }

        this.thereAreIndividuals = columnsToBuild.containsKey(LabelColumn)
                || columnsToBuild.containsKey(IdentifierColumn);
    }

    public Collection<ResourceDTO> getAllResources() {

        buildHierarchies();
        createFromHeaders();

        if (thereAreIndividuals) {
            var individuals = buildIndividuals();
            setLabels(individuals);
            setComments(individuals);
            setTypes(individuals);
            setDataProperties(individuals);
            setObjectRelationships(individuals);

            assert individuals != null;
            resources.addAll(individuals);
        }

        return resources;
    }

    private void buildHierarchies() {

        List<TableAnalysisConcept> hierarchyConcepts = new ArrayList<>(List.of(
                PredefinedClassHierarchyColumn,
                PredefinedDataPropertyHierarchyColumn,
                PredefinedObjectPropertyHierarchyColumn,
                PredefinedAnnotationPropertyHierarchyColumn
        ));

        hierarchyConcepts.forEach(concept -> {
            List<ColumnAnalyzer> columns = columnsToBuild.get(concept);
            if (columns != null) {
                columns.forEach(column -> buildHierarchy(column, concept));
            }
        });
    }

    private void buildHierarchy(ColumnAnalyzer column, TableAnalysisConcept concept) {

        String[] hierarchy = Arrays.stream(column.getColumnData())
                .filter(cellValue -> cellValue != null && !cellValue.isNull())
                .map(CellValue::toString)
                .toArray(String[]::new);

        String hierarchyIndicator = sheetAnalyzer.getHierarchyLevelIndicator();

        if (hierarchy.length == 0
            || hierarchy[0].contains(hierarchyIndicator)) {

            String header = column.getHeader();
            hierarchy = Stream.concat(Stream.of(header), Arrays.stream(hierarchy))
                    .toArray(String[]::new);
        }

        ResourceDTOFactoryProvider factoryProvider = new ResourceDTOFactoryProvider(concept);

        try {
            resources.addAll(HierarchyParser.parseHierarchyFromStringsBeginsWithIndicator(
                    hierarchy, hierarchyIndicator, factoryProvider));
        } catch (RuntimeException e) {
            try {
                resources.addAll(HierarchyParser.parseHierarchyFromStringsContainsIndicator(
                        hierarchy, hierarchyIndicator, factoryProvider));
            } catch (Exception ex) {
                resources.addAll(HierarchyParser.parseHierarchyFlat(
                        hierarchy, hierarchyIndicator, factoryProvider));
            }
        }
    }

    private void createFromHeaders() {

        List<TableAnalysisConcept> columnsWithSignificantHeaders = new ArrayList<>(List.of(
                DataPropertyColumn,
                ObjectPropertyColumn
        ));

        if (thereAreIndividuals
                && !columnsToBuild.containsKey(PredefinedClassHierarchyColumn))
            columnsWithSignificantHeaders.add(ClassColumn);

        columnsWithSignificantHeaders.forEach(concept -> {
            List<ColumnAnalyzer> columns = columnsToBuild.get(concept);
            if (columns != null) {
                columns.forEach(column ->
                        resources.add(ResourceDTOFactoryProvider.getInstance(concept)
                                .create(column.getHeader())));
            }
        });
    }

    private List<IndividualDTO> buildIndividuals() {

        if (columnsToBuild.containsKey(IdentifierColumn)) {
            return createIndividualsUsing(IdentifierColumn);
        }
        else if (columnsToBuild.containsKey(LabelColumn)) {
            return createIndividualsUsing(LabelColumn);
        }

        return null;
    }

    private List<IndividualDTO> createIndividualsUsing(TableAnalysisConcept concept) {

        List<IndividualDTO> individuals = new ArrayList<>();

        CellValue[] identifierValues = columnsToBuild.get(concept)
                .getFirst()
                .getColumnData();

        Arrays.stream(identifierValues)
                .filter(Objects::nonNull)
                .forEach(cellValue -> individuals.add(
                        IndividualDTOFactory.createIndividual(cellValue)));

        return individuals;
    }


    private void setLabels(List<IndividualDTO> individuals) {

        if (!columnsToBuild.containsKey(LabelColumn))
            return;

        CellValue[] labels = columnsToBuild.get(LabelColumn).getFirst().getColumnData();

        for (int i = 0; i < labels.length; i++) {

            if (labels[i] == null || labels[i].isNull())
                continue;

            IndividualDTO individual = individuals.get(i);
            String labelValue = labels[i].toString();
            AnnotationPropertyDTO idLabel = new AnnotationPropertyDTO(AnnotationPropertyType.label, individual.getId());

            if (individual.getAnnotationProperties() != null
                && individual.getAnnotationProperties().contains(idLabel))
                individual.removeAnnotationProperty(idLabel);

            individual.addLabel(labelValue);
        }
    }

    private void setComments(List<IndividualDTO> individuals) {

        if (!columnsToBuild.containsKey(CommentColumn))
            return;

        CellValue[] comments = columnsToBuild.get(CommentColumn).getFirst().getColumnData();

        for (int i = 0; i < comments.length; i++) {

            if (comments[i] == null || comments[i].isNull())
                continue;

            individuals.get(i).addComment(comments[i].toString());
        }
    }

    private void setTypes(List<IndividualDTO> individuals) {

        if (!columnsToBuild.containsKey(ClassColumn))
            return;

        for (ColumnAnalyzer classColumn : columnsToBuild.get(ClassColumn)) {

            CellValue[] classes = classColumn.getColumnData();

            for (int rowId = 0; rowId < classes.length; rowId++) {

                CellValue classCell = classes[rowId];

                if (classCell == null || classCell.isNull())
                    continue;

                if (rowId >= individuals.size())
                    continue;

                IndividualDTO individual = individuals.get(rowId);

                if (individual == null)
                    continue;

                if (classCell.getType() == CellType.BOOLEAN) {

                    if (classCell.getBoolean())
                        individual.addType(new ClassDTO(classColumn.getHeader()));

                    continue;
                }

                List<String> tokens = classCell.getTokenizedValues();

                individual.addAllTypes(tokens.stream()
                        .map(ClassDTO::new)
                        .toList());
            }
        }

        for (IndividualDTO individual : individuals) {

            if (individual == null
            || !individual.getTypes().isEmpty())
                continue;

            List<ColumnAnalyzer> labelColumnList = columnsToBuild.get(LabelColumn);

            if (labelColumnList != null) {
                ColumnAnalyzer labelColumn = labelColumnList.getFirst();
                individual.addType(new ClassDTO(labelColumn.getHeader()));
            } else {
                individual.addType(new ClassDTO(sheetAnalyzer.getSheetName()));
            }
        }
    }

    private void setDataProperties(List<IndividualDTO> individuals) {

        if (!columnsToBuild.containsKey(DataPropertyColumn))
            return;

        for (ColumnAnalyzer dataPropertyColumn : columnsToBuild.get(DataPropertyColumn)) {
            CellValue[] dataProperties = dataPropertyColumn.getColumnData();
            String dataPropertyId = dataPropertyColumn.getHeader();

            Class<?> type = Object.class;

            for (int rowId = 0; rowId < dataProperties.length; rowId++) {
                CellValue cellValue = dataProperties[rowId];
                if (cellValue == null || cellValue.isNull()) continue;

                DataPropertyDTO dataProperty = DataPropertyDTOFactory.createDataProperty(
                        dataPropertyId, cellValue, type
                );

                individuals.get(rowId).addDataProperty(dataProperty);
            }
        }
    }

    private void setObjectRelationships(List<IndividualDTO> individuals) {

        if (!columnsToBuild.containsKey(ObjectPropertyColumn)) return;

        columnsToBuild.get(ObjectPropertyColumn).forEach(objectPropertyColumn -> {
            var identifierColumnContainer = columnsToBuild.get(IdentifierColumn);
            if (identifierColumnContainer != null)
                addObjectProperties(individuals, identifierColumnContainer.getFirst(), objectPropertyColumn);

            var labelColumnContainer = columnsToBuild.get(LabelColumn);
            if (labelColumnContainer != null)
                addObjectProperties(individuals, labelColumnContainer.getFirst(), objectPropertyColumn);
        });
    }

    private void addObjectProperties(
            List<IndividualDTO> individuals,
            ColumnAnalyzer identifier,
            ColumnAnalyzer objectPropertyColumn) {

        CellValue[] identifierColumnData = identifier.getColumnData();
        CellValue[] objPropColumnData = objectPropertyColumn.getColumnData();
        String objPropertyName = objectPropertyColumn.getHeader();

        for (int rowId = 0; rowId < identifierColumnData.length; rowId++) {
            CellValue subjectValue = identifierColumnData[rowId];
            if (subjectValue == null || subjectValue.isNull()) continue;

            IndividualDTO foundSubject = findIndividual(individuals, subjectValue);
            if (foundSubject == null) continue;

            CellValue objectValue = objPropColumnData[rowId];
            if (objectValue == null || objectValue.isNull()) continue;

            IndividualDTO foundObject = findIndividual(individuals, objectValue);
            if (foundObject == null) continue;

            foundSubject.addObjectProperty(new ObjectPropertyDTO(objPropertyName, foundObject));
        }
    }

    private IndividualDTO findIndividual(List<IndividualDTO> individuals, CellValue cellValue) {
        return individuals.stream()
                .filter(ind -> Objects.equals(ind, IndividualDTOFactory.createIndividual(cellValue)))
                .findFirst()
                .orElse(null);
    }

    public Map<? extends ClassDTO, ? extends ClassDTO> getClassBindingPairs() {

        var result = new HashMap<ClassDTO, ClassDTO>();

        if (!columnsToBuild.containsKey(ClassBindingSourceColumn)
            || !columnsToBuild.containsKey(ClassBindingTargetColumn))
            return result;

        var sourceColumnValues = columnsToBuild.get(ClassBindingSourceColumn).getFirst().getColumnData();

        for (var targetColumn : columnsToBuild.get(ClassBindingTargetColumn)) {

            for (int rowId = 0; rowId < sourceColumnValues.length; rowId++) {

                var sourceTokens = sourceColumnValues[rowId].getTokenizedValues();
                var targetTokens = targetColumn.getColumnData()[rowId].getTokenizedValues();

                for (String sourceToken : sourceTokens) {

                    for (String targetToken : targetTokens) {
                        result.put(
                                new ClassDTO(sourceToken),
                                new ClassDTO(targetToken));
                    }
                }
            }
        }

        return result;
    }
}
