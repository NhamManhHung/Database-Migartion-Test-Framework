package auto.framework.utils;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CsvStreamWriter<T> implements AutoCloseable {

    private final Writer writer;
    private final StatefulBeanToCsv<T> beanToCsv;
    private final List<T> buffer = new ArrayList<>();
    private final int batchSize;
    private static final Map<Class<?>, String[]> HEADER_CACHE = new ConcurrentHashMap<>();
    public CsvStreamWriter(
            String filePath,
            Class<T> type,
            char delimiter,
            int batchSize
    ) throws Exception {
        this.batchSize = batchSize;

        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        this.writer = new BufferedWriter(new FileWriter(filePath), 64 * 1024);

        String[] columnOrder = HEADER_CACHE.computeIfAbsent(type, t -> {
            Field[] fields = type.getDeclaredFields();
            return Arrays.stream(fields)
                    .map(Field::getName)
                    .toArray(String[]::new);
        });

        writer.write(String.join(String.valueOf(delimiter), columnOrder));
        writer.write("\n");

        ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
        strategy.setType(type);
        strategy.setColumnMapping(columnOrder);

        this.beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                .withMappingStrategy(strategy)
                .withSeparator(delimiter)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .build();
    }

    public void write(T row) throws Exception {
        buffer.add(row);
        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    private void flush() throws Exception {
        if (!buffer.isEmpty()) {
            beanToCsv.write(buffer);
            buffer.clear();
            writer.flush();
        }
    }

    @Override
    public void close() throws Exception {
        flush();
        writer.close();
    }
}
