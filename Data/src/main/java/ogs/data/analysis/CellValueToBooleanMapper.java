package ogs.data.analysis;

import ogs.data.core.CellValue;

import java.util.HashMap;
import java.util.Map;

public class CellValueToBooleanMapper {

    private final static Map<String, Boolean> stringMap = new HashMap<>(
        Map.of(
            "+", true,
            "-", false,
            "Yes", true,
            "No", false,
            "Так", true,
            "Ні", false
        )
    );

    public static Boolean tryMap(CellValue cellValue) {

        if (cellValue == null)
            return null;

        switch (cellValue.getType()) {
            case BOOLEAN -> {
                return cellValue.getBoolean();
            }
            case STRING -> {
                return stringMap.getOrDefault(cellValue.getString(), null);
            }
            case NUMERIC -> {
                if (cellValue.getValue() instanceof Double value) {
                    if (value == 1.0) return true;
                    if (value == 0.0) return false;
                }
                return null;
            }
            default -> {
                return null;
            }
        }
    }
}
