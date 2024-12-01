package ogs.startup;

import ogs.ui.OntologyGenerationApplication;

public class Main {
    public static void main(String[] args) {

        final String configFilePath = "configuration.json";

        OntologyGenerationApplication.setConfigFilePath(configFilePath);
        OntologyGenerationApplication.run(args);
    }
}