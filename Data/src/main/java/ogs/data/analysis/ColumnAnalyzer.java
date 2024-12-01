package ogs.data.analysis;

import ogs.data.core.CellValue;
import ogs.model.core.RegExPatterns;
import ogs.model.core.TableAnalysisConcept;
import org.apache.poi.ss.usermodel.CellType;

import java.util.*;

public class ColumnAnalyzer {

    // Data
    private int columnIndex;
    private String header;
    private CellValue[] columnData;

    // Metrics
    private double completeness = Double.NaN;
    private double uniqueness = Double.NaN;
    private double uniquenessIgnoringNulls = Double.NaN;

    private double tokenUniqueness = Double.NaN;

    private double wordAverage = Double.NaN;
    private double wordMedian = Double.NaN;

    // Conclusion
    private TableAnalysisConcept definition = null;
    private String reason = null;

    public void update(CellValue[] column, int index) {

        columnIndex = index;
        header = column[0].toString();
        columnData = Arrays.stream(column)
                .skip(1)
                .toArray(CellValue[]::new);
    }

    public void measureAll() {

        measureBasic();
        measureTokens();
        measureWords();
    }

    public void measureBasic() {

        int columnLength = columnData.length;

        if (columnLength == 0) {
            completeness = Double.NaN;
            uniqueness = Double.NaN;
            uniquenessIgnoringNulls = Double.NaN;
            return;
        }

        Set<Object> uniqueValues = new HashSet<>(columnLength);
        int nullCount = 0;

        for (CellValue cellValue : columnData) {

            // Completeness
            if (cellValue == null || cellValue.isNull()) {
                nullCount++;
                continue;
            }

            // Uniqueness
            uniqueValues.add(cellValue.getValue());
        }

        int uniqueCount = uniqueValues.size();

        completeness = 1.0 - (double) nullCount / columnLength;
        uniqueness = (double) uniqueCount / columnLength;
        uniquenessIgnoringNulls = (columnLength - nullCount) >= 0
                ? (double) (uniqueCount - (uniqueValues.contains(null) ? 1 : 0))
                    / (columnLength - nullCount)
                : 0;
    }

    public void measureTokens() {

        int columnLength = columnData.length;

        if (columnLength == 0) {
            tokenUniqueness = Double.NaN;
            return;
        }

        Set<String> uniqueTokens = new HashSet<>(columnLength);
        int tokenCount = 0;

        for (CellValue cellValue : columnData) {

            if (cellValue == null || cellValue.isNull())
                continue;

            var tokens = Arrays.stream(cellValue.toString()
                    .split(RegExPatterns.TOKENIZATION))
                    .toList();

            uniqueTokens.addAll(tokens);
            tokenCount += tokens.size();
        }

        int uniqueTokenCount = uniqueTokens.size();

        tokenUniqueness = (double) uniqueTokenCount / tokenCount;
    }

    public void measureWords() {

        if (columnData.length == 0) {
            wordAverage = Double.NaN;
            wordMedian = Double.NaN;
            return;
        }

        var wordCounts = new ArrayList<Integer>(columnData.length);

        for (CellValue cellValue : columnData) {

            if (cellValue == null
                || cellValue.isNull()
                || cellValue.getType() != CellType.STRING)
                continue;

            String[] words = cellValue.toString().split("\\s*");

            wordCounts.add(words.length);
        }

        int size = wordCounts.size();

        if (size == 0)
            return;

        Collections.sort(wordCounts);

        this.wordAverage = wordCounts.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);

        if (size % 2 == 1) {
            this.wordMedian = wordCounts.get(size / 2);
        } else {
            int middle1 = wordCounts.get(size / 2 - 1);
            int middle2 = wordCounts.get(size / 2);
            this.wordMedian = (middle1 + middle2) / 2.0;
        }
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public String getHeader() {
        return header;
    }

    public CellValue[] getColumnData() {
        return columnData;
    }

    public double getCompleteness() {
        return completeness;
    }

    public double getUniqueness() {
        return uniqueness;
    }

    public double getUniquenessIgnoringNulls() {
        return uniquenessIgnoringNulls;
    }

    public double getTokenUniqueness() {
        return tokenUniqueness;
    }

    public double getWordAverage() {
        return wordAverage;
    }

    public double getWordMedian() {
        return wordMedian;
    }

    public TableAnalysisConcept getDefinition() {
        return definition;
    }

    public void setDefinition(TableAnalysisConcept definition) {
        this.definition = definition;
    }

    public boolean isDefined() {
        return definition != null;
    }

    public boolean isDefinedAs(TableAnalysisConcept definition) {
        return definition == this.definition;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
