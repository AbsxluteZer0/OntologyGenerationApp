package ogs.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextAnalyzerService {

    public static List<String> extractReferences(String description, String[] lookupPatterns) {

        List<String> references = new ArrayList<>();

        for (String pattern : lookupPatterns) {
            String regex = pattern + " ([^.]*?)\\.";
            Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(description);

            while (m.find()) {
                String match = m.group(1).trim();
                String[] items = match.split("(,? and |, |; )");
                for (String item : items) {
                    references.add(item.trim());
                }
            }
        }

        return references;
    }
}
