package ogs.model.ontology;

public class ClassDTO extends HierarchicalDTO<ClassDTO> {

    public ClassDTO(String displayName) {
        this(Vocabulary.local, displayName);
    }

    public ClassDTO(Vocabulary namespace, String displayName) {
        super(namespace, capitalizeFirstAlphabeticalChar(displayName));
        addLabel(capitalizeFirstAlphabeticalChar(displayName));
    }

    private static String capitalizeFirstAlphabeticalChar(String input) {

        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input);
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (Character.isAlphabetic(c)) {
                result.setCharAt(i, Character.toUpperCase(c));
                break;
            }
        }

        return result.toString();
    }
}
