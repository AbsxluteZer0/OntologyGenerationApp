package ogs.data.core;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;

public class SheetDataExtractor {

    private final Sheet sheet;

    public SheetDataExtractor(Sheet sheet) {
        this.sheet = sheet;
    }

    public CellValue[][] extractValues() {

        int rowCount = sheet.getLastRowNum() + 1;
        if (rowCount <= 0) return null;

        List<List<CellValue>> rowValues = new ArrayList<>(rowCount);

        for (int rowNum = sheet.getFirstRowNum(); rowNum < rowCount; rowNum++) {

            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            short cellCount = row.getLastCellNum();
            if (cellCount <= 0) continue;

            // Find the last valid cell (with content)
            short actualCellCount = (short) Math.max(cellCount, getLastValidCellInRow(row));

            List<CellValue> cellValues = new ArrayList<>(actualCellCount);

            // Add missing cells as nulls for any uninitialized cells at the beginning
            for (short cellNum = 0; cellNum < actualCellCount; cellNum++) {
                Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                cellValues.add(new CellValue(cell));
            }

            sheet.removeRow(row);
            rowValues.add(cellValues);
        }

        return rowValues.stream()
                .map(innerList -> innerList.toArray(new CellValue[0]))
                .toArray(CellValue[][]::new);
    }

    private short getLastValidCellInRow(Row row) {
        short lastValidCell = -1;
        for (short cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                lastValidCell = cellNum;
            }
        }
        return (short) (lastValidCell + 1);  // Adjust so it’s the number of cells, not the index
    }

    public String getSheetName() {
        return sheet.getSheetName();
    }
}
