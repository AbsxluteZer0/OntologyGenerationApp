package ogs.data.core;

import ogs.model.core.RegExPatterns;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.apache.poi.ss.usermodel.CellType.*;
import static org.apache.poi.ss.usermodel.DateUtil.*;

public class CellValue {

    private final Object value;
    private CellType type;

    public CellValue(Double value) {
        this.value = value;
        this.type = NUMERIC;
    }

    public CellValue(String value) {

        if (value == null || value.isBlank()) {
            this.type = _NONE;
            this.value = null;
        } else {
            this.type = STRING;
            this.value = value;
        }
    }

    public CellValue(Cell cell) {

        if (cell == null) {
            type = _NONE;
            value = null;
        }
        else {
            type = cell.getCellType();
            value = extractValue(cell, type);
        }
    }

    public CellValue(Boolean value) {

        if (value == null) {
            this.type = _NONE;
            this.value = null;
        } else {
            this.type = BOOLEAN;
            this.value = value;
        }
    }

    private Object extractValue(Cell cell, CellType cellType) {

        return switch (cellType) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> isCellDateFormatted(cell)
                    ? cell.getDateCellValue()
                    : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> {
                type = cell.getCachedFormulaResultType();
                yield extractValue(cell, type);
            }
            default -> {
                type = _NONE;
                yield null;
            }
        };
    }

    /**
     * Returns this cell type which hints on its inner value type. The possible types are:
     * {@code BOOLEAN}, {@code NUMERIC}, {@code STRING}, and {@code _NONE}.
     *
     * @return the {@link CellType} of this cell, which is always one of BOOLEAN, NUMERIC, STRING, or _NONE.
     */
    public CellType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Boolean getBoolean() throws ClassCastException {
        return (Boolean) value;
    }

    public Date getDate() throws ClassCastException {
        return (Date) value;
    }

    public Double getDouble() throws ClassCastException {
        return (Double) value;
    }

    public String getString() throws ClassCastException {
        return (String) value;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean isOfType(Class<?> type) {
        return value.getClass().equals(type);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public List<String> getTokenizedValues() {

        if (value == null || type != CellType.STRING)
            return new ArrayList<>();

        return Arrays.stream(getString()
                        .split(RegExPatterns.TOKENIZATION))
                .map(String::strip)
                .toList();
    }

    public static CellValue concatenate(CellValue value1, CellValue value2) {

        if (value1 == null || value1.isNull())
            return value2;
        if (value2 == null || value2.isNull())
            return value1;

        if (value1.getType() == NUMERIC
            && value2.getType() == NUMERIC) {

            if (value1.isOfType(Date.class))
                return new CellValue(value1.toString() + value2);
            if (value1.getDouble() == 0)
                return value2;

            return new CellValue(
                    Double.valueOf(value1.getDouble().toString() + value2.getDouble()));
        }

        return new CellValue(value1.toString() + value2);
    }
}
