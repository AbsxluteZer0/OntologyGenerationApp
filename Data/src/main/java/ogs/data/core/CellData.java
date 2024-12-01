package ogs.data.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper class for 2D cell data providing a convenient interface for row-wise
 * and column-wise access to cell values. This class encapsulates a 2D array of
 * {@link CellValue} objects and offers methods to retrieve and modify the data
 * either by row or by column. Consider minimizing alternating row-column updates
 * to improve performance.
 */
public class CellData {

    // Two perspectives of the same data
    private CellValue[][] rowValues;
    private CellValue[][] columnValues;
    private boolean rowValuesAreUpToDate = true;
    private boolean columnValuesAreUpToDate = true;
    private final List<ModelChangeListener> listeners = new ArrayList<>();
    private final int numberOfRowsToSkip = 1; // to skip the first (header) row

    public CellData(CellValue[][] rowPerspectiveData) {
        setRowValues(rowPerspectiveData);
    }

    public void addModelChangeListener(ModelChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyModelChanged() {
        for (ModelChangeListener listener : listeners) {
            listener.onModelChanged();
        }
    }

    private void columnValuesChanged() {
        rowValuesAreUpToDate = false;
        columnValuesAreUpToDate = true;
        notifyModelChanged();
    }

    private void rowValuesChanged() {
        columnValuesAreUpToDate = false;
        rowValuesAreUpToDate = true;
        notifyModelChanged();
    }

    private void ensureColumnValuesAreUpToDate() {

        if (columnValuesAreUpToDate)
            return;

        columnValues = transpose(rowValues);
        columnValuesAreUpToDate = true;
    }

    private void ensureRowValuesAreUpToDate() {

        if (rowValuesAreUpToDate)
            return;

        rowValues = transpose(columnValues);
        rowValuesAreUpToDate = true;
    }

    /**
     * @return The transposed matrix.
     * Adds null values where those were missing to complete the rectangular matrix.
     */
    private CellValue[][] transpose(CellValue[][] matrix) {

        int rowCount = matrix.length;
        int rowMaxLength = 0;
        for (CellValue[] row : matrix) {
            if (row.length > rowMaxLength) {
                rowMaxLength = row.length;
            }
        }

        var transposed = new CellValue[rowMaxLength][rowCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }

        return transposed;
    }

    //region Getters and setters
    /**
     * Retrieves the cell data in row-wise format.
     *
     * @return a 2D array of {@link CellValue} objects organized by rows.
     */
    public CellValue[][] getRowValues() {

        ensureRowValuesAreUpToDate();

        return rowValues;
    }

    /**
     * Sets the cell data in row-wise format.
     *
     * @param rowValues a 2D array of {@link CellValue} objects to be organized by rows.
     */
    public void setRowValues(CellValue[][] rowValues) {

        if (rowValues == null)
            rowValues = new CellValue[0][];

        this.rowValues = rowValues;

        rowValuesChanged();
    }

    /**
     * Retrieves the cell data in column-wise format.
     *
     * @return a transposed 2D array of {@link CellValue} objects organized by columns.
     */
    public CellValue[][] getColumnValues() {

        ensureColumnValuesAreUpToDate();

        return columnValues;
    }

    /**
     * Sets the cell data in column-wise format.
     *
     * @param columnValues a 2D array of {@link CellValue} objects to be organized by columns.
     */
    public void setColumnValues(CellValue[][] columnValues) {

        if (columnValues == null)
            columnValues = new CellValue[0][];

        this.columnValues = columnValues;

        columnValuesChanged();
    }

    public CellValue[] getColumn(int columnId) {

        ensureColumnValuesAreUpToDate();

        return columnValues[columnId];
    }

    public CellValue[] getColumnWithoutHeader(int columnId) {

        ensureColumnValuesAreUpToDate();

        return Arrays.stream(columnValues[columnId])
                .skip(numberOfRowsToSkip)
                .toArray(CellValue[]::new);
    }

    public CellValue[] getRow(int rowId) {

        ensureRowValuesAreUpToDate();

        return rowValues[rowId];
    }

    public int getNumberOfColumns() {

        ensureColumnValuesAreUpToDate();

        return columnValues.length;
    }
    //endregion
}
