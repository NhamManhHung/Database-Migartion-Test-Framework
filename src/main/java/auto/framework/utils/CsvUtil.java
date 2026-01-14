package auto.framework.utils;

import auto.framework.models.dto.TableInfoCSV;
import com.opencsv.CSVWriter;
import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class CsvUtil<T> {

    private final Class<T> type;
    private final char delimiter;

    public CsvUtil(Class<T> type) {
        this(type, CSVWriter.DEFAULT_SEPARATOR);
    }

    public CsvUtil(Class<T> type, char delimiter) {
        this.type = type;
        this.delimiter = delimiter;
    }

    /**
     * ================== READ NO BATCH ==================
     **/
//    public List<T> read(String filePath) throws IOException {
//        try (Reader reader = new FileReader(filePath)) {
//
//            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
//            strategy.setType(type);
//
//            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
//                    .withMappingStrategy(strategy)
//                    .withSeparator(delimiter)
//                    .withIgnoreLeadingWhiteSpace(true)
//                    .withThrowExceptions(false)
//                    .build();
//
//            List<T> list = csvToBean.parse();
//
//            if (!csvToBean.getCapturedExceptions().isEmpty()) {
//                System.out.println("Skipped invalid rows: " + csvToBean.getCapturedExceptions().size());
//            }
//
//            return list;
//        }
//    }

    private List<T> readInternal(Reader reader) {
        HeaderColumnNameMappingStrategy<T> strategy =
                new HeaderColumnNameMappingStrategy<>();
        strategy.setType(type);

        CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                .withMappingStrategy(strategy)
                .withSeparator(delimiter)
                .withIgnoreLeadingWhiteSpace(true)
                .withThrowExceptions(false)
                .build();

        List<T> list = csvToBean.parse();

        if (!csvToBean.getCapturedExceptions().isEmpty()) {
            System.out.println("Skipped invalid rows: "
                    + csvToBean.getCapturedExceptions().size());
        }

        return list;
    }

    public List<T> read(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            return readInternal(reader);
        }
    }
    public List<T> read(InputStream is) throws IOException {
        try (Reader reader =
                     new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return readInternal(reader);
        }
    }



    /**
     * ================== WRITE NO BATCH ==================
     **/
    public void write(String filePath, List<T> list)
            throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        try (Writer writer = new FileWriter(filePath)) {

            Field[] fields = type.getDeclaredFields();
            String[] columnOrder = java.util.stream.Stream.of(fields).map(Field::getName).toArray(String[]::new);

            writer.write(String.join(String.valueOf(delimiter), columnOrder));
            writer.write("\n");

            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(type);
            strategy.setColumnMapping(columnOrder);

            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withMappingStrategy(strategy)
                    .withSeparator(delimiter)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();

            beanToCsv.write(list);
        }
    }

    /**
     * ================== WRITE BATCH ==================
     **/
    public void writeBatch(String filePath, Consumer<Consumer<List<T>>> batchFeeder)
            throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        try (Writer writer = new FileWriter(filePath)) {

            Field[] fields = type.getDeclaredFields();
            String[] columnOrder = java.util.stream.Stream.of(fields).map(Field::getName).toArray(String[]::new);

            writer.write(String.join(String.valueOf(delimiter), columnOrder));
            writer.write("\n");

            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(type);
            strategy.setColumnMapping(columnOrder);

            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withMappingStrategy(strategy)
                    .withSeparator(delimiter)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();

            Consumer<List<T>> batchWriter = batch -> {
                try {
                    if (batch != null && !batch.isEmpty()) {
                        beanToCsv.write(batch);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            batchFeeder.accept(batchWriter);
        }
    }
    public static void exportResolvedPkCsv(
            String outputPath,
            List<TableInfoCSV> tables,
            Map<String, List<String>> detectedPk
    ) {

        try (Writer writer =
                     new OutputStreamWriter(
                             new FileOutputStream(outputPath),
                             StandardCharsets.UTF_8)) {

            Class<TableInfoCSV> type = TableInfoCSV.class;

            Field[] fields = type.getDeclaredFields();
            String[] columnOrder = Arrays.stream(fields)
                    .map(Field::getName)
                    .toArray(String[]::new);

            writer.write(String.join(
                    String.valueOf(CSVWriter.DEFAULT_SEPARATOR),
                    columnOrder
            ));
            writer.write("\n");

            ColumnPositionMappingStrategy<TableInfoCSV> strategy =
                    new ColumnPositionMappingStrategy<>();
            strategy.setType(type);
            strategy.setColumnMapping(columnOrder);

            StatefulBeanToCsv<TableInfoCSV> beanToCsv =
                    new StatefulBeanToCsvBuilder<TableInfoCSV>(writer)
                            .withMappingStrategy(strategy)
                            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .build();

            List<TableInfoCSV> resolvedRows = new ArrayList<>();

            for (TableInfoCSV row : tables) {

                TableInfoCSV copy = new TableInfoCSV();
                copy.setTcKeyCount(row.getTcKeyCount());
                copy.setTcKeyDuplicate(row.getTcKeyDuplicate());
                copy.setTcKeyMatching(row.getTcKeyMatching());
                copy.setTableName(row.getTableName());

                String pk = row.getPrimaryKeyColumn();

                if (pk == null || pk.equalsIgnoreCase("null")) {
                    List<String> autoPk =
                            detectedPk.get(row.getTableName());
                    if (autoPk != null) {
                        pk = String.join("|", autoPk);
                    }
                }

                copy.setPrimaryKeyColumn(pk);
                resolvedRows.add(copy);
            }

            beanToCsv.write(resolvedRows);

        } catch (Exception e) {
            throw new RuntimeException("Export resolved PK CSV failed", e);
        }
    }

}
