package ogs.data.analysis;

import ogs.data.core.CellData;
import ogs.data.core.CellValue;
import ogs.data.core.ModelChangeListener;
import ogs.data.core.SheetDataExtractor;
import ogs.model.core.AnalysisKeywordDictionary;
import ogs.model.core.RegExPatterns;
import ogs.model.core.TableAnalysisConcept;
import org.apache.poi.ss.usermodel.CellType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ogs.model.core.TableAnalysisConcept.*;

public class SheetAnalyzer implements ModelChangeListener {

    // Core
    private Queue<Runnable> analysisSteps;
    private final SheetDataExtractor dataExtractor;
    private final SheetDataTransformer dataTransformer;
    private final AnalysisKeywordDictionary keywords;
    private final String hierarchyLevelIndicator;
    private CellData cellData;
    private boolean modelIsUpToDate = false;

    // Analysis results
    private String sheetName;
    /** Consider making headerRow String[]. In {@code this::extractHeaderRow};
     You can safely use {@code CellValue::toString}
     or explicitly {@code CellValue::getString},
     relying on {@code this::defineHeaderRow} to find STRING-only row.
     */
    private CellValue[] headerRow;
    private ColumnAnalyzer[] columns;

    public SheetAnalyzer(SheetDataExtractor dataExtractor,
                         AnalysisKeywordDictionary keywords,
                         String hierarchyLevelIndicator) {

        if (dataExtractor == null)
            throw new IllegalArgumentException("Data extractor must be initialized!");

        this.dataExtractor = dataExtractor;
        dataTransformer = new SheetDataTransformer();
        this.keywords = Objects.requireNonNullElseGet(keywords, AnalysisKeywordDictionary::new);
        this.hierarchyLevelIndicator = hierarchyLevelIndicator;
    }

    public void initialize() {

        sheetName = dataExtractor.getSheetName();
        CellValue[][] rowValues = dataExtractor.extractValues();

        // Remove null/blank rows
        rowValues = dataTransformer.filterEmptyArrays(rowValues);

        cellData = new CellData(rowValues);

        // Remove null/blank columns
        CellValue[][] columnValues = cellData.getColumnValues();
        columnValues = dataTransformer.filterEmptyArrays(columnValues);
        cellData.setColumnValues(columnValues);

        // Remove unwanted characters
        String cleansingPattern = RegExPatterns.getDataCleansingPattern();
        if (cleansingPattern != null && !cleansingPattern.isEmpty()) {
            rowValues = dataTransformer.cleanseData(cellData.getRowValues(), cleansingPattern);
            cellData.setRowValues(rowValues);
        }

        initializeColumnsArray(columnValues.length);

        cellData.addModelChangeListener(this);

        analysisSteps = new LinkedList<>(
                List.of(
                        this::ensureHeaderRowIsTheFirst,
                        this::analyzeHeaderRowForKeywords,
                        this::defineIdentifierColumn,
                        this::defineLabelColumn,
                        this::collapseSpannedRows,
                        this::leverageHierarchies,
                        this::defineObjectPropertyColumns,
                        this::makeBooleanColumnsExplicit,
                        this::defineClassColumns,
                        this::defineCommentColumn,
                        this::defineTheRemainingColumns
                )
        );
    }

    public void analyze() {

        while (!analysisSteps.isEmpty() && !this.completelyDefined()) {
            Objects.requireNonNull(analysisSteps.poll()).run();
        }

        ensureModelIsUpToDate();
    }

    /**
     * @return Index of the first row.
     */
    private int defineHeaderRowId() {
        return 0;

        // If this implementation proves insufficient:

        //new: IF there is null/_NONE, check further up to .25 of rows, then collapse spanned, using first STRING populated
        //// There is no way to collapse spanned columns without a pivot row and the only possible pivot row is the header row
        // 2. Check the first row to be complete and completely of type STRING
        // 3. Continue searching for such row.
        // 3.a if there is such row, assume it is the header row
        // 3.b if there is NO such row, start again, but this time skip null and _NONE
        // 3.b.1 if there still no such row ...

        // 4. Having the header row as pivot, collapse possibly spanned columns
    }

    private void ensureHeaderRowIsTheFirst() {

        CellValue[][] rowValues = cellData.getRowValues();

        int headerRowId = defineHeaderRowId();
        headerRow = rowValues[headerRowId];

        if (headerRowId > 0) {
            rowValues = dataTransformer.removeFirstNSubArrays(rowValues, headerRowId);
            cellData.setRowValues(rowValues);
        }
    }

    private void analyzeHeaderRowForKeywords() {

        for (int i = 0; i < headerRow.length; i++) {

            TableAnalysisConcept concept;
            StringBuilder responseBuilder = new StringBuilder(10);
            CellValue header = headerRow[i];
            responseBuilder.append(header);
            concept = keywords.tryMatch(responseBuilder);

            if (concept == null)
                continue;

            columns[i].setDefinition(concept);
            columns[i].setReason(responseBuilder.toString());
        }
    }

    private void defineIdentifierColumn() {

        if (anyColumnIsDefinedAs(IdentifierColumn))
            return;

        ensureModelIsUpToDate();

        Pattern identifierPattern = Pattern.compile(
                "^(\\p{L}+\\p{N}+|\\p{N}+(\\.\\p{N}+)*\\.?|\\p{N}+\\)|\\p{N}+\\.[ \\p{L}\\p{N}]*)$");

        ColumnAnalyzer[] fullyUniqueColumnsIgnoringNullsUndefined = Arrays.stream(columns)
                .filter(column -> column.getUniquenessIgnoringNulls() >= 1.0
                        && !column.isDefined())
                .toArray(ColumnAnalyzer[]::new);

        for (ColumnAnalyzer column : fullyUniqueColumnsIgnoringNullsUndefined) {

            boolean matches = false;

            for (CellValue cellValue : column.getColumnData()) {

                if (cellValue == null || cellValue.isNull())
                    continue;

                Matcher matcher = identifierPattern.matcher(cellValue.toString());
                if (matcher.matches()) {
                    matches = true;
                } else {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                column.setDefinition(IdentifierColumn);
                column.setReason(String.format(
                        "Every value of the column is unique and matches the regular expression.\nPattern: %s",
                        identifierPattern));
                break;
            }
        }
    }

    private void defineLabelColumn() {

        if (anyColumnIsDefinedAs(LabelColumn))
            return;

        ensureModelIsUpToDate();

        final double uniquenessThreshold = 0.9;

        ColumnAnalyzer[] highlyUniqueColumnsIgnoringNullsUndefined = Arrays.stream(columns)
                .filter(column -> column.getUniquenessIgnoringNulls() >= uniquenessThreshold
                        && !column.isDefined())
                .toArray(ColumnAnalyzer[]::new);

        for (ColumnAnalyzer column : highlyUniqueColumnsIgnoringNullsUndefined) {

            boolean matches = false;

            for (CellValue cellValue : column.getColumnData()) {

                if (cellValue == null || cellValue.isNull())
                    continue;

                if (cellValue.getType() == CellType.STRING) {
                    matches = true;
                } else {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                column.setDefinition(LabelColumn);
                column.setReason(String.format(
                        "The non-null values of the column have uniqueness >= %.2f and are of type STRING.",
                        uniquenessThreshold));
                break;
            }
        }
    }

    private void collapseSpannedRows() {

        ensureModelIsUpToDate();

        ColumnAnalyzer pivotColumn = getColumnDefinedAs(LabelColumn);

        if (pivotColumn == null)
            pivotColumn = getColumnDefinedAs(IdentifierColumn);

        if (pivotColumn == null)
            return;

        CellValue[][] rowValues = cellData.getRowValues();
        rowValues = dataTransformer.collapseSpannedSubArrays(rowValues, pivotColumn.getColumnIndex());
        cellData.setRowValues(rowValues);
    }

    // Headers of both hierarchies and columns can also be checked
    private void leverageHierarchies() {

        ensureModelIsUpToDate();

        final int matchCountThreshold = 3;

        for (ColumnAnalyzer classHierarchyColumn : getColumnsDefinedAs(PredefinedClassHierarchyColumn)) {

            if (classHierarchyColumn != null) {

                String[] flatHierarchyValues = Arrays.stream(
                                classHierarchyColumn.getColumnData())
                        .filter(colData -> colData != null && !colData.isNull())
                        .map(colData -> colData.toString()
                                .replaceAll(hierarchyLevelIndicator, "")
                                .strip())
                        .toArray(String[]::new);

                for (var column : columns) {

                    if (column.isDefined())
                        continue;

                    int matchCount = 0;

                    for (var cell : column.getColumnData()) {

                        if (cell.getType() != CellType.STRING)
                            break;

                        var tokens = cell.getTokenizedValues();

                        for (String flatHierarchyValue : flatHierarchyValues) {
                            if (tokens.contains(flatHierarchyValue))
                                if (++matchCount >= matchCountThreshold) {
                                    column.setDefinition(ClassColumn);
                                    column.setReason(String.format(
                                            "The tokenized values of the column contain at least %d matches of " +
                                                    "the predefined class hierarchy column '%s' with index %d." +
                                                    "\nThe tokenization pattern used is: %s",
                                            matchCountThreshold,
                                            classHierarchyColumn.getHeader(),
                                            classHierarchyColumn.getColumnIndex() + 1,
                                            RegExPatterns.TOKENIZATION
                                    ));
                                }
                        }
                    }
                }
            }
        }

        forEachColumnDefinedAsifFoundColumnValuesMatchInHeaderSetDefinitionAndReason(
                PredefinedDataPropertyHierarchyColumn,
                DataPropertyColumn,
                "Header of the column matches an element of " +
                "the predefined data property hierarchy column '%s' with index %d ignoring case.");

        forEachColumnDefinedAsifFoundColumnValuesMatchInHeaderSetDefinitionAndReason(
                PredefinedObjectPropertyHierarchyColumn,
                ObjectPropertyColumn,
                "Header of the column matches an element of " +
                "the predefined object property hierarchy column '%s' with index %d ignoring case."
        );
    }

    private void forEachColumnDefinedAsifFoundColumnValuesMatchInHeaderSetDefinitionAndReason(
            TableAnalysisConcept concept,
            TableAnalysisConcept definition,
            String reasonTemplate) {

        for (ColumnAnalyzer hierarchyColumn : getColumnsDefinedAs(concept)) {

            String[] flatHierarchyValues = Arrays.stream(hierarchyColumn.getColumnData())
                    .filter(colData -> colData != null && !colData.isNull())
                    .map(colData -> colData.toString()
                            .replaceAll(hierarchyLevelIndicator, "")
                            .strip())
                    .toArray(String[]::new);

            for (var column : columns) {
                for (String flatHierarchyValue : flatHierarchyValues) {
                    if (flatHierarchyValue.equalsIgnoreCase(column.getHeader().strip())) {
                        column.setDefinition(definition);
                        column.setReason(String.format(
                                reasonTemplate,
                                hierarchyColumn.getHeader(),
                                hierarchyColumn.getColumnIndex() + 1
                        ));
                        break;
                    }
                }
            }
        }
    }

    private void defineObjectPropertyColumns() {

        ensureModelIsUpToDate();

        ColumnAnalyzer identifierColumn = getColumnDefinedAs(IdentifierColumn);

        if (identifierColumn == null)
            identifierColumn = getColumnDefinedAs(LabelColumn);

        if (identifierColumn == null)
            return;

        List<String> identifierColumnValues = Arrays.stream(identifierColumn.getColumnData())
                .filter(cellValue -> cellValue != null && !cellValue.isNull())
                .map(cellValue -> cellValue.toString().strip())
                .toList();

        for (var column : columns) {
            
            if (column.isDefined())
                continue;

            // Tokenized match threshold
            int matchCountThreshold = 0;
            int columnSize = column.getColumnData().length;

            for (CellValue cellValue : column.getColumnData()) {

                if (cellValue == null || cellValue.isNull())
                    continue;

                List<String> tokens = cellValue.getTokenizedValues().stream()
                        .map(String::strip)
                        .toList();

                matchCountThreshold += (int) tokens.stream()
                        .filter(identifierColumnValues::contains)
                        .count();

                // Break early if threshold is reached
                if (matchCountThreshold >= columnSize / 2) {
                    column.setDefinition(ObjectPropertyColumn);
                    column.setReason(String.format(
                            "At least half of the tokenized values of the column match values from %s '%s' with index %d.",
                            identifierColumn.getDefinition().getDisplayString(),
                            identifierColumn.getHeader(),
                            identifierColumn.getColumnIndex()
                    ));
                    break;
                }
            }
        }
    }

    private void makeBooleanColumnsExplicit() {

        ensureModelIsUpToDate();

        final double uniquenessThresholdToBecomeBoolean = 0.25;

        Integer[] extremelyNonUniqueUndefinedColumnIDs = Arrays.stream(columns)
                .filter(column -> !column.isDefined()
                        && column.getUniqueness() <= uniquenessThresholdToBecomeBoolean)
                .map(ColumnAnalyzer::getColumnIndex)
                .toArray(Integer[]::new);

        CellValue[][] columnValues = cellData.getColumnValues();
        columnValues = dataTransformer.tryMapSubArraysToBoolean(columnValues, extremelyNonUniqueUndefinedColumnIDs);
        cellData.setColumnValues(columnValues);
    }

    private void defineClassColumns() {

        ensureModelIsUpToDate();

        final double tokenUniquenessThreshold = 0.3;
        final double completenessThreshold = 0.8;

        ColumnAnalyzer[] highlyCompleteUndefinedColumnsWithLowTokenUniqueness = Arrays.stream(columns)
                .filter(column -> !column.isDefined()
                        && column.getTokenUniqueness() <= column.getUniquenessIgnoringNulls()
                        && column.getTokenUniqueness() <= tokenUniquenessThreshold
                        && column.getCompleteness() >= completenessThreshold)
                .toArray(ColumnAnalyzer[]::new);

        for (ColumnAnalyzer inferredClassColumn : highlyCompleteUndefinedColumnsWithLowTokenUniqueness) {

            inferredClassColumn.setDefinition(ClassColumn);
            inferredClassColumn.setReason(String.format(
                    "The column has token uniqueness <= %s and completeness >= %s",
                    tokenUniquenessThreshold,
                    completenessThreshold
            ));
        }
    }

    private void defineCommentColumn() {

        if (anyColumnIsDefinedAs(CommentColumn))
            return;

        ensureModelIsUpToDate();

        ColumnAnalyzer[] undefinedColumnSortedByAverageWords = Arrays.stream(columns)
                .filter(col -> !col.isDefined())
                .sorted(Comparator.comparingDouble(ColumnAnalyzer::getWordAverage).reversed())
                .toArray(ColumnAnalyzer[]::new);

        if (undefinedColumnSortedByAverageWords.length <= 1)
            return;

        final double differenceFactor = 2.0;
        ColumnAnalyzer mostWordyColumn = undefinedColumnSortedByAverageWords[0];
        ColumnAnalyzer nextWordyColumn = undefinedColumnSortedByAverageWords[1];

        if (mostWordyColumn.getWordMedian()
                >= nextWordyColumn.getWordMedian() * differenceFactor) {

            mostWordyColumn = columns[mostWordyColumn.getColumnIndex()];

            mostWordyColumn.setDefinition(CommentColumn);
            mostWordyColumn.setReason(String.format(
                    "The column has most words on average (%.2f words) and its word median (%.1f words) " +
                    "is at least %.1f times higher than the one of the column with the next most words on average " +
                    "(mean = %.2f words, median = %.1f words)",
                    mostWordyColumn.getWordAverage(),
                    mostWordyColumn.getWordMedian(),
                    differenceFactor,
                    nextWordyColumn.getWordAverage(),
                    nextWordyColumn.getWordMedian()
            ));
        }

        // Висока унікальність токенів?
    }

    private void defineTheRemainingColumns() {

        ensureModelIsUpToDate();

        for (ColumnAnalyzer column : columns) {
            if (!column.isDefined()) {
                column.setDefinition(DataPropertyColumn);
                column.setReason("Defined by residual principle.");
            }
        }
    }

    //region Helper methods

    private boolean completelyDefined() {

        return columns != null
                && Arrays.stream(columns)
                    .allMatch(ColumnAnalyzer::isDefined);
    }

    @Override
    public void onModelChanged() {
        modelIsUpToDate = false;
    }

    private void ensureModelIsUpToDate() {

        if (modelIsUpToDate)
            return;

        int actualNumberOfColumns = cellData.getNumberOfColumns();

        if (actualNumberOfColumns > columns.length)
            initializeColumnsArray(actualNumberOfColumns);

        for (int columnId = 0; columnId < actualNumberOfColumns; columnId++) {

            columns[columnId].update(cellData.getColumn(columnId), columnId);
            columns[columnId].measureAll();
        }

        modelIsUpToDate = true;
    }

    private void initializeColumnsArray(int newLength) {

        if (columns == null) {
            columns = new ColumnAnalyzer[newLength];
            for (int i = 0; i < newLength; i++) {
                columns[i] = new ColumnAnalyzer();
            }
            return;
        }

        ColumnAnalyzer[] newColumns = Arrays.copyOf(columns, newLength);

        for (int i = columns.length; i < newLength; i++)
            newColumns[i] = new ColumnAnalyzer();

        columns = newColumns;
    }

    private boolean anyColumnIsDefinedAs(TableAnalysisConcept definition) {
        return Arrays.stream(getColumnAnalyzers())
                .anyMatch(column ->
                        column.isDefinedAs(definition));
    }

    private ColumnAnalyzer getColumnDefinedAs(TableAnalysisConcept definition) {

        for(var column : columns) {
            if (column.isDefinedAs(definition))
                return column;
        }

        return null;
    }

    private ColumnAnalyzer[] getColumnsDefinedAs(TableAnalysisConcept definition) {

        return Arrays.stream(columns)
                .filter(column -> column.isDefinedAs(definition))
                .toArray(ColumnAnalyzer[]::new);
    }

    //endregion

    public String getSheetName() {
        return sheetName;
    }

    public ColumnAnalyzer[] getColumnAnalyzers() {
        return columns;
    }

    public String getHierarchyLevelIndicator() {
        return hierarchyLevelIndicator;
    }
}