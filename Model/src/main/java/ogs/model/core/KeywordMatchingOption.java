package ogs.model.core;

public enum KeywordMatchingOption {

    CompleteMatch ("Complete match"),
    CaseSensitive ("Case sensitive");

    private final String displayString;

    KeywordMatchingOption(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }
}
