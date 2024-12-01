package ogs.localization;

import ogs.model.ontology.AnnotationPropertyType;
import ogs.model.ontology.AnnotationPropertyDTO;
import ogs.model.ontology.ResourceDTO;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

/**
 * The {@code OntologyLabelLocalizationService} class provides functionality for localizing labels
 * of ontology resources by attaching terms in multiple languages. It reads a dictionary table
 * in the form of a two-dimensional {@code String[][]} array, where the first row contains
 * language tags and subsequent rows contain terms in different languages corresponding to each tag.
 *
 * <p>This class is designed to take an array of dictionary data, parse it, and build a mapping of
 * language tags to arrays of terms. Using this mapping, it can attach all available language labels
 * to a list of {@link ResourceDTO} objects that contain initial labels in specific languages.
 *
 * <p>Example usage:
 * <pre>
 *     String[][] dictionaryRows = {
 *         {"en", "uk", "fr"},
 *         {"apple", "яблуко", "pomme"},
 *         {"tree", "дерево", "arbre"}
 *     };
 *
 *     OntologyLabelLocalizationService service = new OntologyLabelLocalizationService(dictionaryRows);
 *     service.attachAllPossibleLabelsTo(ontologyResourceDTOs);
 * </pre>
 *
 * <p>The resulting ontology resources will contain labels in all languages present in the dictionary,
 * based on a case-insensitive match with existing labels in any language.
 */
public class OntologyLabelLocalizationService {

    private final String[][] dictionary;

    /**
     * @param dictionaryRows a 2D array where {@code dictionaryRows[0]} is the header row with language tags,
     *                       and all other rows contain terms corresponding to these languages.
     */
    public OntologyLabelLocalizationService(String[][] dictionaryRows) {

        this.dictionary = dictionaryRows;
    }

    public void attachAllPossibleLabelsTo(List<ResourceDTO> resources) {

        for (ResourceDTO resource : resources) {

            if (resource == null
                || resource.getAnnotationProperties() == null
                || resource.getAnnotationProperties().isEmpty())
                continue;

            AnnotationPropertyDTO[] labels = resource.getAnnotationProperties()
                    .stream()
                    .filter(property -> property.getType() == AnnotationPropertyType.label)
                    .toArray(AnnotationPropertyDTO[]::new);

            List<AnnotationPropertyDTO> labelsToRemove = new ArrayList<>(labels.length);

            for (var label : labels) {

                String langTag = label.getLangTag();
                String processedLabelValue = label.getValue().toLowerCase();
                Integer foundTermIndex = null;

                if (langTag == null) {

                    ImmutablePair<String, Integer> coordinates = findTerm(processedLabelValue);
                    langTag = Objects.requireNonNullElse(coordinates.left, Language.presume(processedLabelValue));
                    foundTermIndex = coordinates.right;

                    addLabelIfMissing(resource, label.getValue(), langTag);

                    labelsToRemove.add(label);
                }
                else {
                    foundTermIndex = findTermInSpecificLanguage(processedLabelValue, langTag);
                }

                if (foundTermIndex == null)
                    continue;

                for (int columnId = 0; columnId < dictionary[foundTermIndex].length; columnId++) {

                    String translation = dictionary[foundTermIndex][columnId];

                    if (translation == null || translation.isBlank())
                        continue;

                    String translationLangTag = dictionary[0][columnId];

                    addLabelIfMissing(resource, translation, translationLangTag);
                }
            }

            for (var labelToRemove : labelsToRemove)
                resource.removeAnnotationProperty(labelToRemove);
        }
    }

    private static void addLabelIfMissing(ResourceDTO resource, String term, String langTag) {

        if (resource.getAnnotationProperty(AnnotationPropertyType.label, langTag) == null) {
            resource.addAnnotationProperty(AnnotationPropertyType.label, term, langTag);
        }
    }

    private ImmutablePair<String, Integer> findTerm(String term) {

        String language = null;
        Integer rowMatchIndex = null;

        for (int rowId = 1; rowId < dictionary.length; rowId++) {

            for (int columnId = 0; columnId < dictionary[rowId].length; columnId++) {

                if (dictionary[rowId][columnId].strip().equalsIgnoreCase(term)) {
                    language = dictionary[0][columnId];
                    rowMatchIndex = rowId;
                    break;
                }
            }
        }

        return new ImmutablePair<>(language, rowMatchIndex);
    }

    private Integer findTermInSpecificLanguage(String term, String languageTag) {

        Integer matchIndex = null;
        int languageColumnIndex = -1;

        for (int columnId = 1; columnId < dictionary[0].length; columnId++) {

            if (dictionary[0][columnId].strip().equalsIgnoreCase(languageTag)) {
                languageColumnIndex = columnId;
                break;
            }
        }

        if (languageColumnIndex == -1) {
            return null;
        }

        for (int rowId = 1; rowId < dictionary.length; rowId++) {

            if (dictionary[rowId][languageColumnIndex].strip().equalsIgnoreCase(term)) {
                matchIndex = rowId;
                break;
            }
        }

        return matchIndex;
    }

    public String createLocalizedOntologyDescription(String langTag, String creator, String lastModifiedByUser, String description, String keywords) {

        if (langTag == null)
            return null;

        StringBuilder sb = new StringBuilder();

        if (langTag.contains("uk")) {
            sb.append("Автор джерела цієї онтології — ");
            sb.append(creator);

            if (isNotNullOrBlank(lastModifiedByUser)
                    && !lastModifiedByUser.equals(creator)) {
                sb.append(". Остання зміна від — ");
                sb.append(lastModifiedByUser);
            }

            if (isNotNullOrBlank(description)) {
                sb.append(".\n\nОпис: ");
                sb.append(description);
            }

            if (isNotNullOrBlank(keywords)) {
                sb.append(".\nКлючові слова: ");
                sb.append(keywords);
            }

            sb.append('.');
        }
        else  {
            // Default to english
            sb.append("Ontology source by ");
            sb.append(creator);

            if (isNotNullOrBlank(lastModifiedByUser)
                    && !lastModifiedByUser.equals(creator)) {
                sb.append(". Last modified by ");
                sb.append(lastModifiedByUser);
            }

            if (isNotNullOrBlank(description)) {
                sb.append(".\n\nDescription: ");
                sb.append(description);
            }

            if (isNotNullOrBlank(keywords)) {
                sb.append(".\nKeywords: ");
                sb.append(keywords);
            }

            sb.append('.');
        }

        return sb.toString();
    }

    private static boolean isNotNullOrBlank(String str) {
        return str != null && !str.isBlank();
    }
}
