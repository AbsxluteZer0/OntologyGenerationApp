package ogs.model.core;

public class RegExPatterns {

    public static final String TOKENIZATION = "[.,;!?]\\s*";

    public static final String PATH_VALIDATION = "[<>:\"/|?*\\\\]+";

    private static String dataCleansingPattern = "";

    public static String getDataCleansingPattern() {
        return dataCleansingPattern;
    }

    public static void setDataCleansingPattern(String dataCleansingPattern) {

        if (dataCleansingPattern == null)
            return;

        RegExPatterns.dataCleansingPattern = dataCleansingPattern;
    }
}
