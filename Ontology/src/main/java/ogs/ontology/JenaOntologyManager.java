package ogs.ontology;

import ogs.model.core.Configuration;
import ogs.model.ontology.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDFS;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

public class JenaOntologyManager {

    public final String PERSONAL_ONTOLOGY_DOMAIN
            = String.format("http://www.semanticweb.org/%s/ontologies/%s/",
            StringUtils.capitalize(System.getProperty("user.name")
                    .strip()
                    .replaceAll("[.!@#$%^&*()_=+-]", "")),
            Year.now());
    private final String FILE_PATH;
    public final String URI_SEPARATOR = "#";
    private String BASE_URI;
    private String COMPLETE_BASE_URI;

    private OntModel model;

    public JenaOntologyManager(Configuration config) throws InvalidPathException {

        String fileName = config.getOntologyFileName();
        String outputDirectoryPath = config.getOutputDirectory();

        if (fileName == null || fileName.isBlank()) {
            fileName = FilenameUtils.getBaseName(config.getSourceFilePath());
        }

        if (outputDirectoryPath == null || outputDirectoryPath.isBlank()) {
            outputDirectoryPath = System.getProperty("user.dir");
        }

        FILE_PATH = Paths.get(outputDirectoryPath, fileName + ".rdf").toString();
    }

    public void initialize() throws RiotException, IOException {

        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        OntologyFileIO.loadFromRDF(model, FILE_PATH);
    }

    public void populateWith(List<ResourceDTO> ontologyResources) {

        for (ClassDTO classDTO : ontologyResources.stream()
                .filter(res -> res instanceof ClassDTO)
                .map(res -> (ClassDTO) res)
                .toList()) {
            addClassHierarchy(classDTO);
        }

        for (DataPropertyDTO dataPropertyDTO : ontologyResources.stream()
                .filter(res -> res instanceof DataPropertyDTO)
                .map(res -> (DataPropertyDTO) res)
                .toList()) {
            addPropertyHierarchy(dataPropertyDTO);
        }

        for (ObjectPropertyDTO objectPropertyDTO : ontologyResources.stream()
                .filter(res -> res instanceof ObjectPropertyDTO)
                .map(res -> (ObjectPropertyDTO) res)
                .toList()) {
            addPropertyHierarchy(objectPropertyDTO);
        }

        for (AnnotationPropertyDTO annotationPropertyDTO : ontologyResources.stream()
                .filter(res -> res instanceof AnnotationPropertyDTO)
                .map(res -> (AnnotationPropertyDTO) res)
                .toList()) {
            addPropertyHierarchy(annotationPropertyDTO);
        }

        for (IndividualDTO individualDTO : ontologyResources.stream()
                .filter(res -> res instanceof IndividualDTO)
                .map(res -> (IndividualDTO) res)
                .toList()) {
            extractOntIndividual(individualDTO);
        }
    }

    public void bindClasses(Map<ClassDTO, ClassDTO> classBindingMap) {

        if (classBindingMap == null || classBindingMap.isEmpty())
            return;

        for (var entry : classBindingMap.entrySet()) {
            ClassDTO keyClassDTO = entry.getKey();
            ClassDTO valueClassDTO = entry.getValue();

            OntClass keyClass = extractOntClass(keyClassDTO);
            OntClass valueClass = extractOntClass(valueClassDTO);

            if (keyClass == null || valueClass == null) {
                throw new IllegalArgumentException("Could not find OntClass for provided ClassDTOs");
            }

            bindInstancesRecursively(keyClass, valueClass);
        }
    }

    private void bindInstancesRecursively(OntClass sourceClass, OntClass targetClass) {

        sourceClass.listInstances().forEachRemaining(individual -> {
            if (individual.isResource()) {
                individual.addRDFType(targetClass);
            }
        });

        sourceClass.listSubClasses(true).forEachRemaining(subClass -> {
            if (subClass.canAs(OntClass.class)) {
                bindInstancesRecursively(subClass.as(OntClass.class), targetClass);
            }
        });
    }

    public void save() throws IOException { OntologyFileIO.saveTo(model, FILE_PATH); }

    public void addClassHierarchy(ClassDTO rootDTO) {

        OntClass root = extractOntClass(rootDTO);
        addClassHierarchyRecursive(rootDTO, root);
    }

    private void addClassHierarchyRecursive(ClassDTO dto, OntClass ontClass) {

        if (dto.getDescendants() != null) {

            for (ClassDTO childDTO : dto.getDescendants()) {

                OntClass childClass = extractOntClass(childDTO);
                ontClass.addSubClass(childClass);
                addClassHierarchyRecursive(childDTO, childClass);
            }
        }
    }

    public <T extends HierarchicalDTO<T>> void addPropertyHierarchy(T rootDTO) {
        OntProperty root = extractOntProperty(rootDTO);
        addHierarchyRecursive(rootDTO, root);
    }

    private <T extends HierarchicalDTO<T>> void addHierarchyRecursive(T dto, OntProperty ontProperty) {

        if (dto.getDescendants() != null) {

            for (T descendant : dto.getDescendants()) {

                OntProperty subProperty = extractOntProperty(descendant);
                ontProperty.addSubProperty(subProperty);
                addHierarchyRecursive(descendant, subProperty);
            }
        }
    }

    private <T extends HierarchicalDTO<T>> OntProperty extractOntProperty(T propertyDTO) {

        OntProperty ontProperty = switch (propertyDTO) {
            case DataPropertyDTO dataPropertyDTO ->
                    model.createDatatypeProperty(resolveURIFor(propertyDTO));

            case ObjectPropertyDTO objectPropertyDTO ->
                    model.createObjectProperty(resolveURIFor(propertyDTO));

            case AnnotationPropertyDTO annotationPropertyDTO ->
                    model.createAnnotationProperty(resolveURIFor(propertyDTO));

            case null ->
                throw new NullPointerException("PropertyDTO is null! Check the value before passing it here!");

            default ->
                    throw new RuntimeException("Unknown property type: " + propertyDTO.getClass().getSimpleName());
        };

        transferAnnotationProperties(propertyDTO, ontProperty);

        return ontProperty;
    }

    private OntClass extractOntClass(ClassDTO ontClassDTO) {

        OntClass ontClass = model.createClass(resolveURIFor(ontClassDTO));

        transferAnnotationProperties(ontClassDTO, ontClass);

        return ontClass;
    }

    public void addIndividual(IndividualDTO individualDTO) {

        Individual individual = extractOntIndividual(individualDTO);
        transferObjectProperties(individualDTO, individual);
    }

    private Individual extractOntIndividual(IndividualDTO individualDTO) {

        List<ClassDTO> types = individualDTO.getTypes();

        if (types == null || types.isEmpty())
            throw new RuntimeException(
                    String.format("The type for the individual %s was not provided.", individualDTO.getId()));

        OntClass firstType = extractOntClass(types.getFirst());

        Individual individual = model.createIndividual(resolveURIFor(individualDTO), firstType);

        types.removeFirst();

        if (!types.isEmpty()) {
            for (ClassDTO typeDTO : types) {

                OntClass ontClass = extractOntClass(typeDTO);
                individual.addRDFType(ontClass);
            }
        }

        transferAnnotationProperties(individualDTO, individual);
        transferDataProperties(individualDTO, individual);

        return individual;
    }

    private void transferObjectProperties(IndividualDTO individualDTO, Individual individual) {

        List<ObjectPropertyDTO> objectProperties = individualDTO.getObjectProperties();

        if (objectProperties == null)
            return;

        for (ObjectPropertyDTO objectPropertyDTO : individualDTO.getObjectProperties()) {

            String propertyURI = resolveURIFor(objectPropertyDTO);
            OntProperty objectProperty = model.getOntProperty(propertyURI);

            if (objectProperty == null) {
                throw new RuntimeException(
                        String.format("Data property %s could not be found in the ontology.", propertyURI));
            }

            Individual object = extractOntIndividual(individualDTO);

            individual.addProperty(objectProperty, object);
        }
    }

    private void transferDataProperties(IndividualDTO individualDTO, Individual individual) {

        List<DataPropertyDTO> dataProperties = individualDTO.getDataProperties();

        if (dataProperties == null)
            return;

        for (DataPropertyDTO dataPropertyDTO : individualDTO.getDataProperties()) {

            String propertyURI = resolveURIFor(dataPropertyDTO);
            OntProperty dataProperty = model.getOntProperty(propertyURI);

            if (dataProperty == null) {
                throw new RuntimeException(
                        String.format("Data property %s could not be found in the ontology.", propertyURI));
            }

            Object value = dataPropertyDTO.getValue();

            switch (value) {
                case null -> { continue; }
                case Integer i ->
                        individual.addLiteral(dataProperty, model.createTypedLiteral(i, XSDDatatype.XSDint));
                case Double d ->
                        individual.addLiteral(dataProperty, model.createTypedLiteral(d, XSDDatatype.XSDdouble));
                case Boolean b ->
                        individual.addLiteral(dataProperty, model.createTypedLiteral(b, XSDDatatype.XSDboolean));
                case Date date -> {
                    String dateValue = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
                    individual.addLiteral(dataProperty, model.createTypedLiteral(dateValue, XSDDatatype.XSDdateTime));
                }
                case String s ->
                        individual.addLiteral(dataProperty, model.createTypedLiteral(s, XSDDatatype.XSDstring));
                default -> throw new RuntimeException(
                        String.format("Unsupported data type for property %s: %s",
                                dataPropertyDTO.getId(),
                                value.getClass().getSimpleName()));
            }
        }
    }

    private void transferAnnotationProperties(ResourceDTO resourceDTO, Resource resource) {

        List<AnnotationPropertyDTO> annotationProperties = resourceDTO.getAnnotationProperties();

        if (annotationProperties == null)
            return;

        for (var annotationProperty : annotationProperties) {
            String value = annotationProperty.getValue();
            String langTag = annotationProperty.getLangTag();

            switch (annotationProperty.getType()) {
                case label -> {
                    if (langTag != null) {
                        resource.addProperty(RDFS.label, resource.getModel().createLiteral(value, langTag));
                    } else {
                        resource.addProperty(RDFS.label, value);
                    }
                }
                case comment -> {
                    if (langTag != null) {
                        resource.addProperty(RDFS.comment, resource.getModel().createLiteral(value, langTag));
                    } else {
                        resource.addProperty(RDFS.comment, value);
                    }
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported annotation property type: " + annotationProperty.getType());
            }
        }
    }

    public Individual resolveIndividual(IndividualDTO individualDTO) {
        return model.getIndividual(resolveURIFor(individualDTO));
    }

    public void createAssociations(List<IndividualDTO> individualDTOs) {

        String associatesWithPropertyURI = COMPLETE_BASE_URI + "associatesWith";
        Property associatesWithProperty = model.getObjectProperty(associatesWithPropertyURI);

        //associatesWith
        for (IndividualDTO individualDTO : individualDTOs) {

            List<IndividualDTO> associatesWithList =
                    analyzeDescriptionForAssociations(individualDTO, individualDTOs);

            if (associatesWithList.isEmpty())
                continue;

            Individual thisIndividual = resolveIndividual(individualDTO);

            if (thisIndividual == null)
                continue;

            if (associatesWithProperty == null) {
                associatesWithProperty = model.createObjectProperty(associatesWithPropertyURI);
            }

            for (IndividualDTO associatesWithDTO : associatesWithList) {

                Individual associatesWithIndividual = resolveIndividual(associatesWithDTO);
                thisIndividual.addProperty(associatesWithProperty, associatesWithIndividual);
            }
        }
    }

    private List<IndividualDTO> analyzeDescriptionForAssociations(IndividualDTO individualDTO,
                                                                  List<IndividualDTO> possibleReferences) {

        List<IndividualDTO> referencedIndividuals = new ArrayList<>();
        Map<String, IndividualDTO> nameToDTOMap = possibleReferences.stream()
                .filter(dto -> dto.getLabel() != null)
                .collect(Collectors.toMap(
                        ResourceDTO::getLabel,
                        obj -> obj,
                        (existing, duplicate) -> existing
                ));


        // Sort keys by length in descending order
        List<String> sortedKeys = nameToDTOMap.keySet().stream()
                .sorted((k1, k2) -> Integer.compare(k2.length(), k1.length()))
                .toList();

        for (String key : sortedKeys) {

            if (individualDTO.equals(nameToDTOMap.get(key))) continue;

            String description = individualDTO.getAnnotationProperty(AnnotationPropertyType.comment);

            if (description == null) continue;

            if (description.contains(" " + key.trim() + " ")
                    || description.contains(" " + key.trim() + ".")
                    || description.contains(" " + key.trim() + ",")
                    || description.contains(" " + key.trim() + ";")
                    || description.endsWith(" " + key.trim())) {
                referencedIndividuals.add(nameToDTOMap.get(key));
            }
        }

        return referencedIndividuals;
    }

    private List<IndividualDTO> analyzeDescriptionForIntegrations(String description,
                                                                  List<IndividualDTO> possibleReferences) {

        List<IndividualDTO> referencedIndividuals = new ArrayList<>();
        Map<String, IndividualDTO> nameToDTOMap = possibleReferences.stream()
                .collect(Collectors.toMap(dto -> dto.getLabel("en"), obj -> obj));

        String[] patterns = {
                "integrates with",
                "integrates seamlessly with",
                "integrations include",
                "integrated with",
                "integration with",
                "integrations with",
                "integrations with tools like",
                "tools, such as"
        };

        List<String> referenceStrings = TextAnalyzerService.extractReferences(description, patterns);

        for (String reference : referenceStrings) {

            nameToDTOMap.forEach((key, value) -> {
                if (key.equals(reference)) {
                    referencedIndividuals.add(value);
                }
            });
        }

        return referencedIndividuals;
    }

    public void setOntologyMetadata() {

        if (model == null)
            throw new IllegalStateException("The model hasn't been initialized yet. Call loadOrCreateModel() first.");

        BASE_URI = resolveBaseURI();
        COMPLETE_BASE_URI = BASE_URI + URI_SEPARATOR;

        // Add the <owl:Ontology rdf:about="baseURI"> tag
        model.createOntology(BASE_URI);

        String fileBaseName = FilenameUtils.getBaseName(FILE_PATH)
                .trim()
                .replaceAll(" ", "-");

        String baseNamespace = generateBaseNamespacePrefix(fileBaseName);

        if (model.getNsPrefixURI(baseNamespace) == null) {
            model.setNsPrefix(baseNamespace, COMPLETE_BASE_URI);
            System.out.printf("Base xmlns is set to: %s=\"%s\"%n", baseNamespace, model.getNsPrefixURI(baseNamespace));
        }
    }

    private String resolveBaseURI() {

        if (model == null)
            throw new IllegalStateException("The model hasn't been initialized yet. Call initialize() first.");

        String baseURI = null;

        if (model.listOntologies().hasNext()) {
            Ontology ontology = model.listOntologies().next();
            baseURI = ontology.getURI();
        }

        if (baseURI == null) {
            System.out.println("Base URI not found in ontology definition. It will be inferred from the ontology file name.");
            baseURI = PERSONAL_ONTOLOGY_DOMAIN
                    + FilenameUtils.getBaseName(FILE_PATH)
                    .trim()
                    .replaceAll(" ", "-");
            System.out.println("Base URI is set to: " + baseURI);
        }

        return baseURI;
    }

    private String generateBaseNamespacePrefix(String fileBaseName) {

        String[] wordsToOmit = {"and", "of", "the", "for", "in", "on", "at", "by", "with"};

        StringBuilder nsAliasBuilder = new StringBuilder();
        String[] words = fileBaseName.split("-");

        for (String word : words) {
            if (!word.isEmpty()) {
                String lowerCaseWord = word.toLowerCase();

                // Only append the first letter if the word is not in the wordsToOmit list
                if (!Arrays.asList(wordsToOmit).contains(lowerCaseWord)) {
                    String sanitizedPart = lowerCaseWord.replaceAll("[^\\p{L}\\p{N}]", "");
                    nsAliasBuilder.append(sanitizedPart.charAt(0));
                }
            }
        }

        return nsAliasBuilder.toString();
    }

    private String resolveURIFor(ResourceDTO resourceDTO) {
        return COMPLETE_BASE_URI + resourceDTO.getId();
    }

    public void addDataPropertiesByNames(List<String> dataPropertyNames) {

        for (String dataPropertyName : dataPropertyNames) {
            model.createDatatypeProperty(COMPLETE_BASE_URI + dataPropertyName);
        }
    }

    private boolean isNotNullOrBlank(String str) {
        return str != null && !str.isBlank();
    }
}
