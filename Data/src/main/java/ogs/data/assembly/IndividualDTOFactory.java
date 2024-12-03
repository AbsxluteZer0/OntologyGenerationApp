package ogs.data.assembly;

import ogs.data.core.CellValue;
import ogs.model.ontology.IndividualDTO;

public class IndividualDTOFactory {

    public static String formatIdentifier(CellValue cellValue) {

        if (cellValue == null || cellValue.isNull()) {
            throw new IllegalArgumentException("CellValue cannot be null or empty");
        }

        return switch (cellValue.getType()) {
            case STRING -> cellValue.getString();
            case NUMERIC -> {
                double value = cellValue.getDouble();
                yield (value == (long) value) ? String.valueOf((long) value) : String.valueOf(value);
            }
            default -> throw new IllegalArgumentException("Unsupported CellType for identifier: " + cellValue.getType());
        };
    }

    public static IndividualDTO createIndividual(CellValue cellValue) {
        String identifier = formatIdentifier(cellValue);
        return new IndividualDTO(identifier, identifier); // Using the same value for id and label
    }
}
