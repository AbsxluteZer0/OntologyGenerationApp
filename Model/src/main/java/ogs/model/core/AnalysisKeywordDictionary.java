package ogs.model.core;

import java.util.*;

public class AnalysisKeywordDictionary {

    private final SortedMap<String, TableAnalysisConcept> keywordsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final SortedMap<String, EnumSet<KeywordMatchingOption>> optionsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public void put(String keyword, TableAnalysisConcept concept) {
        put(keyword, concept, EnumSet.noneOf(KeywordMatchingOption.class));
    }

    public void put(String keyword, TableAnalysisConcept concept, EnumSet<KeywordMatchingOption> options) {

        if (keyword == null || keyword.isBlank())
            return;

        String normalizedKeyword = keyword.strip();

        keywordsMap.put(normalizedKeyword, concept);
        optionsMap.put(normalizedKeyword, options);
    }

    public void remove(String keyword) {

        if (keyword == null)
            return;

        keywordsMap.remove(keyword);
        optionsMap.remove(keyword);
    }

    public TableAnalysisConcept tryMatch(StringBuilder container) {

        String normalizedInput = container.toString().strip();

        for (Map.Entry<String, TableAnalysisConcept> entry : keywordsMap.entrySet()) {

            String keyword = entry.getKey();
            TableAnalysisConcept concept = entry.getValue();
            EnumSet<KeywordMatchingOption> options
                    = optionsMap.getOrDefault(keyword, EnumSet.noneOf(KeywordMatchingOption.class));

            if (matches(normalizedInput, keyword, options)) {

                container.setLength(0);
                container.append("Keyword match found with \"");
                container.append(keyword);
                container.append('\"');

                for (var option : options) {
                    container.append(";\n");
                    container.append(option.getDisplayString());
                    container.append(" = ");
                    container.append(options.contains(option));
                }

                container.append('.');

                return concept;
            }
        }

        return null;
    }

    private boolean matches(String input, String keyword, EnumSet<KeywordMatchingOption> options) {

        boolean caseSensitive = options.contains(KeywordMatchingOption.CaseSensitive);
        boolean completeMatch = options.contains(KeywordMatchingOption.CompleteMatch);

        String processedInput = caseSensitive ? input : input.toLowerCase();
        String processedKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        if (completeMatch)
            return processedInput.equals(processedKeyword);
        else
            return processedInput.contains(processedKeyword);
    }

    public Map<String, TableAnalysisConcept> getKeywordsMap() {
        return keywordsMap;
    }

    public Map<String, EnumSet<KeywordMatchingOption>> getOptionsMap() {
        return optionsMap;
    }
}
