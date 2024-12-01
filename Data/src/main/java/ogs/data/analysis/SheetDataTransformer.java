package ogs.data.analysis;

import ogs.data.core.CellValue;
import org.apache.poi.ss.usermodel.CellType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SheetDataTransformer {

    /**
     * Filters out empty sub-arrays from the provided 2D array of {@link CellValue} objects.
     * A sub-array is considered empty if it contains only null or effectively null elements.
     * An element is effectively null if its inner value is null, as determined
     * by {@code cellValue.isNull()}.
     *
     * <p>This method returns a new 2D array containing only non-empty sub-arrays, i.e., those with
     * at least one non-null and non-effectively-null {@link CellValue} element.
     *
     * @param cellData a 2D array of {@link CellValue} objects.
     * @return a filtered 2D array of {@link CellValue} sub-arrays, excluding any sub-arrays
     *         that contain only null or effectively null elements.
     */
    public CellValue[][] filterEmptyArrays(CellValue[][] cellData) {

        if (cellData == null)
            return null;

        return Arrays.stream(cellData).filter(cells ->
                        Arrays.stream(cells).anyMatch(SheetDataTransformer::isSignificant))
                .toArray(CellValue[][]::new);
    }



    public CellValue[][] removeFirstNSubArrays(CellValue[][] cellData, int numberToRemove) {

        if (cellData == null)
            return null;

        return Arrays.stream(cellData)
                .skip(numberToRemove)
                .toArray(CellValue[][]::new);
    }

    public CellValue[][] collapseSpannedSubArrays(CellValue[][] cellData, int pivotIndex) {

        if (cellData == null
            || cellData.length <= 1
            || pivotIndex < 0
            || pivotIndex > cellData.length)
            return cellData;

        if (Arrays.stream(cellData[0])
                .allMatch(SheetDataTransformer::isEffectivelyNull))
            cellData = filterEmptyArrays(cellData);

        List<CellValue[]> mergedData = new ArrayList<>(cellData.length);
        CellValue[] currentBaseSubArray = cellData[0];

        for (int subArrayId = 0; subArrayId < cellData.length; subArrayId++) {

            var pivotValue = cellData[subArrayId][pivotIndex];

            if (isSignificant(pivotValue) || subArrayId == 0) {
                currentBaseSubArray = cellData[subArrayId];
                mergedData.add(currentBaseSubArray);
                continue;
            }

            for (int elementId = 0; elementId < cellData[subArrayId].length; elementId++) {

                var cellValue = cellData[subArrayId][elementId];

                if (isEffectivelyNull(cellValue))
                    continue;

                currentBaseSubArray[elementId] = CellValue.concatenate(currentBaseSubArray[elementId], cellValue);
            }
        }

        return mergedData.toArray(CellValue[][]::new);
    }

    public CellValue[][] cleanseData(CellValue[][] cellData, String cleansingPattern) {

        if (cellData == null || cellData.length == 0) {
            return cellData;
        }

        for (int i = 0; i < cellData.length; i++) {
            for (int j = 0; j < cellData[i].length; j++) {

                CellValue cellValue = cellData[i][j];

                if (cellValue != null && cellValue.getType() == CellType.STRING) {

                    String cleansedValue = cellValue.getString().replaceAll(cleansingPattern, "");
                    cellData[i][j] = new CellValue(cleansedValue);
                }
            }
        }

        return cellData;
    }

    public CellValue[][] tryMapSubArraysToBoolean(CellValue[][] cellData, Integer[] targetIDs) {

        if (targetIDs == null
            || cellData == null)
            return cellData;

        for (int subArrayId : targetIDs) {

            for (int elementId = 0; elementId < cellData[subArrayId].length; elementId++) {

                Boolean mappedValue = CellValueToBooleanMapper.tryMap(cellData[subArrayId][elementId]);

                if (mappedValue != null)
                    cellData[subArrayId][elementId] = new CellValue(mappedValue);
            }
        }

        return cellData;
    }

    private static boolean isEffectivelyNull(CellValue cellValue) {
        return cellValue == null || cellValue.isNull();
    }

    private static boolean isSignificant(CellValue cellValue) {
        return cellValue != null && !cellValue.isNull();
    }
}
