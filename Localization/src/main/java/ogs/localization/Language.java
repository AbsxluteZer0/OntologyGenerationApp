package ogs.localization;

public class Language {

    public static final String EN = "en";
    public static final String UK = "uk";

    public static String presume(String string) {

        // Contains cyrillic unicode characters
        if (string.matches(".*[\\u0400-\\u04FF].*")) {
            return UK;
        }

        if (string.matches(".*[a-zA-Z].*")) {
            return EN;
        }

        // No cyrillic or latin letters
        return null;
    }
}
