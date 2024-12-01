package ogs.data.assembly;

import ogs.model.ontology.HierarchicalDTO;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

class HierarchyParser {

    public static <T extends HierarchicalDTO<T>> List<T> parseHierarchyFromStringsBeginsWithIndicator(
            String[] hierarchy, String hierarchyIndicator, ResourceDTOFactoryProvider factoryProvider) throws RuntimeException {

        List<T> hierarchicalResources = new ArrayList<>(hierarchy.length);
        List<T> ancestorRecord = new ArrayList<>(hierarchy.length);
        int currentLevel = 0;

        if (hierarchy[0].startsWith(hierarchyIndicator)) {
            throw new RuntimeException(
                    String.format("The first element of the hierarchy starts with the subclass indicator \"%s\".",
                            hierarchyIndicator));
        }

        for (String classString : hierarchy) {
            int level = 0;
            int indicatorLength = hierarchyIndicator.length();

            // Count leading subclass indicators
            while (classString.startsWith(hierarchyIndicator, level * indicatorLength)) {
                level++;
            }

            if (level > currentLevel + 1) {
                throw new RuntimeException(String.format("One or more levels of hierarchy are missing. " +
                        "There are %d subclass indicators, while the previous level is %d", level, currentLevel));
            }

            String id = classString.substring(level * indicatorLength);  // Removing subclass indicators from start
            T currentObject = (T)factoryProvider.getInstance().create(id);

            if (level > 0) {
                T prev = hierarchicalResources.get(hierarchicalResources.indexOf(ancestorRecord.get(level - 1)));
                currentObject.addAncestor(prev);
            } else if (level < currentLevel) {
                T prev = hierarchicalResources.get(hierarchicalResources.indexOf(ancestorRecord.get(level - 1)));
                currentObject.addAncestor(prev);
            } else if (level < 0) {
                throw new RuntimeException("Hierarchy level somehow got less than 0.");
            }

            hierarchicalResources.add(currentObject);
            ancestorRecord.add(level, currentObject);
            currentLevel = level;
        }

        return hierarchicalResources;
    }

    // fall back to
    public static <T extends HierarchicalDTO<T>> List<T> parseHierarchyFromStringsContainsIndicator(
            String[] hierarchy, String hierarchyIndicator, ResourceDTOFactoryProvider factoryProvider) {

        List<T> hierarchicalResources = new ArrayList<>(hierarchy.length);
        List<T> ancestorRecord = new ArrayList<>(hierarchy.length);
        int currentLevel = 0;

        if (hierarchy[0].contains(hierarchyIndicator)) {
            throw new RuntimeException(
                    String.format("The first element of the hierarchy contains the subclass indicator \"%s\".",
                            hierarchyIndicator));
        }

        for (String classString : hierarchy) {

            int level = StringUtils.countMatches(classString, hierarchyIndicator);

            if (level > currentLevel + 1) {
                throw new RuntimeException(String.format("One or more levels of hierarchy is missing. " +
                        "There are %d subclass indicators, while the previous level is %d", level, currentLevel));
            }

            String id = classString.replaceAll(hierarchyIndicator, "");
            T currentObject = (T)factoryProvider.getInstance().create(id);

            if (level == 0) {
                // Do nothing more.
            }
            else if (level > 0) {
                T prev = hierarchicalResources.get(hierarchicalResources.indexOf(ancestorRecord.get(level - 1)));
                currentObject.addAncestor(prev);
            } else if (level < currentLevel) {
                T prev = hierarchicalResources.get(hierarchicalResources.indexOf(ancestorRecord.get(level - 1)));
                currentObject.addAncestor(prev);
            } else if (level <  0) {
                throw new NotImplementedException("Hierarchy level somehow got less than 0.");
            }

            hierarchicalResources.add(currentObject);
            ancestorRecord.add(level, currentObject);
            currentLevel = level;
        }

        return hierarchicalResources;
    }

    // fall back to
    public static <T extends HierarchicalDTO<T>> List<T> parseHierarchyFlat(
            String[] hierarchy, String hierarchyIndicator, ResourceDTOFactoryProvider factoryProvider)
    {
        List<T> hierarchicalResources = new ArrayList<>(hierarchy.length);

        for (String s : hierarchy) {

            if (s == null || s.isBlank())
                continue;

            String id = s.replaceAll(hierarchyIndicator, "");
            T currentObject = (T)factoryProvider.getInstance().create(id);

            hierarchicalResources.add(currentObject);
        }

        return hierarchicalResources;
    }
}
