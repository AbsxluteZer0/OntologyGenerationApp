package ogs.data.assembly;

import ogs.data.core.CellValue;
import ogs.model.ontology.DataPropertyDTO;

import java.util.Date;

public class DataPropertyDTOFactory {

    public static DataPropertyDTO createDataProperty(String id, CellValue cellValue, Class<?> type) {

        if (type != Object.class) {
            return new DataPropertyDTO(id, type.cast(cellValue.getValue()), type);
        }

        return switch (cellValue.getType()) {
            case BOOLEAN -> new DataPropertyDTO(id, cellValue.getBoolean(), Boolean.class);
            case NUMERIC -> {
                if (cellValue.isOfType(Date.class))
                    yield new DataPropertyDTO(id, cellValue.getDate(), Date.class);
                if (cellValue.getDouble() % 1 == 0)
                    yield new DataPropertyDTO(id, cellValue.getValue(), Integer.class);
                yield new DataPropertyDTO(id, cellValue.getDouble(), Double.class);
            }
            case STRING -> new DataPropertyDTO(id, cellValue.getString(), String.class);
            default -> throw new RuntimeException("Unexpected CellValue type");
        };
    }
}
