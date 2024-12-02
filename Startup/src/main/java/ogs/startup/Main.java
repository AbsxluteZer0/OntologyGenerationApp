package ogs.startup;

import ogs.ui.OntologyGenerationApplication;

public class Main {
    public static void main(String[] args) {

        final String configFilePath = "configuration.json";
        final String defaultConfigFilePath = "configuration-default.json";

        OntologyGenerationApplication.setConfigFilePath(configFilePath);
        OntologyGenerationApplication.setDefaultConfigFilePath(defaultConfigFilePath);
        OntologyGenerationApplication.run(args);
    }
}