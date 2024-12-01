package ogs.data;

import ogs.data.analysis.ColumnAnalyzer;
import ogs.data.core.CellValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnAnalyzerTest {

    private ColumnAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ColumnAnalyzer();
    }

    @Test
    void testMeasureBasic() {
        CellValue[] column = {
                new CellValue("Header"),
                new CellValue("Value1"),
                new CellValue("Value2"),
                new CellValue("Value1"),
                new CellValue((String) null),
                new CellValue("Value3"),
        };

        analyzer.update(column, 0);
        analyzer.measureBasic();

        assertEquals(0.8, analyzer.getCompleteness(), 0.01);
        assertEquals(0.6, analyzer.getUniqueness(), 0.01);
        assertEquals(1.0, analyzer.getUniquenessIgnoringNulls(), 0.01);
    }

    @Test
    void testMeasureTokens() {
        CellValue[] column = {
                new CellValue("Header"),
                new CellValue("Token1; Token2"),
                new CellValue("Token3"),
                new CellValue("Token1.Token4"),
                new CellValue((String) null),
        };

        analyzer.update(column, 0);
        analyzer.measureTokens();

        assertEquals(0.667, analyzer.getTokenUniqueness(), 0.01);
    }

    @Test
    void testMeasureTokenUniqueness() {
        CellValue[] column = {
                new CellValue("Header"),
                new CellValue("Token1; Token1"),
                new CellValue("Token1"),
                new CellValue("Token1.Token3"),
                new CellValue("Token1, Token1"),
                new CellValue("Token1! Token1"),
                new CellValue("Token1?"),
                new CellValue((String) null),
        };

        analyzer.update(column, 0);
        analyzer.measureTokens();

        assertEquals(0.2, analyzer.getTokenUniqueness(), 0.01);
    }

    @Test
    void testMeasureWords() {
        CellValue[] column = {
                new CellValue("Header"),
                new CellValue("This is a test"),
                new CellValue("Another test case"),
                new CellValue("Word"),
                new CellValue((String) null),
        };

        analyzer.update(column, 0);
        analyzer.measureWords();

        assertEquals(1.8, analyzer.getWordAverage(), 0.01);
        assertEquals(1.0, analyzer.getWordMedian(), 0.01);
    }

    @Test
    void testMeasureAll() {
        CellValue[] column = {
                new CellValue("Header"),
                new CellValue("Value1"),
                new CellValue("Value2"),
                new CellValue("Value1"),
                new CellValue("Token1 Token2"),
                new CellValue("Another test case"),
                new CellValue((String) null),
        };

        analyzer.update(column, 0);
        analyzer.measureAll();

        assertEquals(0.83, analyzer.getCompleteness(), 0.01);
        assertEquals(0.5, analyzer.getUniqueness(), 0.01);
        assertEquals(0.6, analyzer.getUniquenessIgnoringNulls(), 0.01);
        assertEquals(0.67, analyzer.getTokenUniqueness(), 0.01);
        assertEquals(3.0, analyzer.getWordAverage(), 0.01);
        assertEquals(3.5, analyzer.getWordMedian(), 0.01);
    }

    @Test
    void testEmptyColumn() {
        CellValue[] column = {
                new CellValue("Header")
        };

        analyzer.update(column, 0);
        analyzer.measureAll();

        assertEquals(0.0, analyzer.getCompleteness() );
        assertEquals(0.0, analyzer.getUniqueness());
        assertEquals(0.0, analyzer.getUniquenessIgnoringNulls());
        assertEquals(0.0, analyzer.getTokenUniqueness());
        assertEquals(0.0, analyzer.getWordAverage());
        assertEquals(0.0, analyzer.getWordMedian());
    }

//    @BeforeEach
//    public void setUp() {
//        columnAnalyzer = new ColumnAnalyzer();
//    }
//
//    private CellValue mockCellValue(Object value, boolean isNull) { //explanation in chat
//        CellValue cellValue = Mockito.mock(CellValue.class);
//        Mockito.when(cellValue.isNull()).thenReturn(isNull);
//        Mockito.when(cellValue.getValue()).thenReturn(value);
//        return cellValue;
//    }
//
////    @Test
////    public void testAllNulls() {
////        CellValue[] column = IntStream.range(0, 5)
////                .mapToObj(i -> null)
////                .toArray(CellValue[]::new);
////
////        columnAnalyzer.update(column);
////
////        assertEquals(0.0, columnAnalyzer.getCompleteness(),
////                "Completeness should be 0.0 when all values are null");
////        assertEquals(0.0, columnAnalyzer.getUniqueness(),
////                "Uniqueness should be 0.0 when all values are null");
////        assertEquals(0.0, columnAnalyzer.getUniquenessIgnoringNulls(),
////                "Uniqueness ignoring nulls should be 0.0 when all values are null");
////    }
//
//    @Test
//    public void testAllNullValues() {
//        CellValue[] column = IntStream.range(0, 5)
//                .mapToObj(i -> mockCellValue(null, true))
//                .toArray(CellValue[]::new);
//
//        columnAnalyzer.update(column);
//
//        assertEquals(0.0, columnAnalyzer.getCompleteness(),
//                "Completeness should be 0.0 when all values are null");
//        assertEquals(0.0, columnAnalyzer.getUniqueness(),
//                "Uniqueness should be 0.0 when all values are null");
//        assertEquals(0.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 0.0 when all values are null");
//    }
//
//    @Test
//    public void testNoNullValuesAllUnique() {
//        CellValue[] column = new CellValue[]{
//                mockCellValue("A", false),
//                mockCellValue("B", false),
//                mockCellValue("C", false)
//        };
//
//        columnAnalyzer.update(column);
//
//        assertEquals(1.0, columnAnalyzer.getCompleteness(),
//                "Completeness should be 1.0 when no values are null");
//        assertEquals(0.666, columnAnalyzer.getUniqueness(), 0.001, // ???
//                "Uniqueness should be 2/3");
//        assertEquals(1.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 1.0 when all values are unique and present");
//    }
//
//    @Test
//    public void testNoNullValuesAllSame() {
//        CellValue[] column = IntStream.range(0, 5)
//                .mapToObj(i -> mockCellValue("sameValue", false))
//                .toArray(CellValue[]::new);
//
//        columnAnalyzer.update(column);
//
//        assertEquals(1.0, columnAnalyzer.getCompleteness(),
//                "Completeness should be 1.0 when no values are null");
//        assertEquals(0.0, columnAnalyzer.getUniqueness(),
//                "Uniqueness should be 0.0 when all values are the same");
//        assertEquals(0.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 0.0 when all values are the same");
//    }
//
//    @Test
//    public void testMixedValues() {
//        CellValue[] column = new CellValue[]{
//                mockCellValue(null, true),   // null value
//                mockCellValue("unique1", false),
//                mockCellValue("unique2", false),
//                mockCellValue("unique1", false),
//                mockCellValue(null, true)    // another null value
//        };
//
//        columnAnalyzer.update(column);
//
//        assertEquals(0.6, columnAnalyzer.getCompleteness(), 0.001,
//                "Completeness should be 0.6 when 2 out of 5 values are null");
//        assertEquals(0.4, columnAnalyzer.getUniqueness(), 0.001,
//                "Uniqueness should be 0.4 for two unique values in a column of 5");//??
//        assertEquals(1.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 1.0 for unique non-null values");//??
//    }
//
//    @Test
//    public void testAllValuesAsNoneType() {
//        CellValue[] column = IntStream.range(0, 4)
//                .mapToObj(i -> mockCellValue(null, false))
//                .toArray(CellValue[]::new);
//
//        columnAnalyzer.update(column);
//
//        assertEquals(0.0, columnAnalyzer.getCompleteness(),
//                "Completeness should be 0.0 when all values are present but empty");
//        assertEquals(0.0, columnAnalyzer.getUniqueness(),
//                "Uniqueness should be 0.0 when all values are the same and empty");
//        assertEquals(0.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 0.0 when all values are empty");
//    }
//
//    @Test
//    public void testSingleNonNullValue() {
//        CellValue[] column = new CellValue[]{
//                mockCellValue(null, true),
//                mockCellValue("onlyValue", false),
//                mockCellValue(null, true)
//        };
//
//        columnAnalyzer.update(column);
//
//        assertEquals(0.333, columnAnalyzer.getCompleteness(), 0.001,
//                "Completeness should be 0.333 when one out of three values is non-null");
//        assertEquals(0.333, columnAnalyzer.getUniqueness(), 0.001,
//                "Uniqueness should be 1/3 for one unique value in a column of three");
//        assertEquals(1.0, columnAnalyzer.getUniquenessIgnoringNulls(),
//                "Uniqueness ignoring nulls should be 1.0 when only one unique non-null value is present");
//    }
}