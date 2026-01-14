package auto.framework.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

public class ExcelUtil<T> {

    private final Class<T> clazz;
    private final List<Field> fields;

    public ExcelUtil(Class<T> clazz) {
        this.clazz = clazz;
        this.fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field f : fields) f.setAccessible(true);
    }

    /**
     *
     * @param filePath
     * @return
     * @throws Exception
     */

    ///READ NO BATCH
    public List<T> read(String filePath) throws Exception {
        List<T> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return data;

            Map<String, Integer> columnMap = createColumnMap(headerRow);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) data.add(createInstanceFromRow(row, columnMap));
            }
        }
        return data;
    }

    /** ================== READ BATCH ================== **/
    public void readBatch(String filePath, int batchSize, Consumer<List<T>> batchConsumer) throws Exception {
        List<T> batch = new ArrayList<>(batchSize);
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return;

            Map<String, Integer> columnMap = createColumnMap(headerRow);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    batch.add(createInstanceFromRow(row, columnMap));
                    if (batch.size() >= batchSize) {
                        batchConsumer.accept(new ArrayList<>(batch));
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) batchConsumer.accept(batch);
        }
    }

    /** ================== WRITE NO BATCH ================== **/
    public void write(String filePath, List<T> data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            writeHeader(sheet.createRow(0));
            writeData(sheet, 1, data);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    /** ================== WRITE BATCH (Iterable) ================== **/
//    public void writeBatch(String filePath, Iterable<List<T>> batchSupplier) throws Exception {
//        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); // flush every 100 rows
//             FileOutputStream fos = new FileOutputStream(filePath)) {
//            Sheet sheet = workbook.createSheet("Sheet1");
//            writeHeader(sheet.createRow(0));
//            int rowNum = 1;
//            for (List<T> batch : batchSupplier) {
//                rowNum = writeData(sheet, rowNum, batch);
//            }
//            workbook.write(fos);
//            workbook.dispose();
//        }
//    }
    /** ================== WRITE BATCH ================== **/
    public void writeBatch(String filePath, Consumer<Consumer<List<T>>> batchFeeder) throws Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); // flush every 100 rows
             FileOutputStream fos = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Sheet1");
            writeHeader(sheet.createRow(0));
            int[] rowNum = {1}; // dùng array để mutable trong lambda

            // batchWriter: nhận từng batch và ghi vào sheet
            Consumer<List<T>> batchWriter = batch -> {
                try {
                    if (batch != null && !batch.isEmpty()) {
                        rowNum[0] = writeData(sheet, rowNum[0], batch);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            };

            // gọi batchFeeder với batchWriter
            batchFeeder.accept(batchWriter);

            workbook.write(fos);
            workbook.dispose();
        }
    }
    public void writeBatch(String filePath, List<T> items, int batchSize) throws Exception {
        writeBatch(filePath, batchWriter -> {
            for (int i = 0; i < items.size(); i += batchSize) {
                List<T> batch = items.subList(i, Math.min(i + batchSize, items.size()));
                batchWriter.accept(batch);
            }
        });
    }


    /** ================== PRIVATE HELPERS ================== **/
    private Map<String, Integer> createColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            map.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        }
        return map;
    }

    private T createInstanceFromRow(Row row, Map<String, Integer> columnMap) throws Exception {
        T obj = clazz.getDeclaredConstructor().newInstance();
        for (Field f : fields) {
            Integer col = columnMap.get(f.getName().toLowerCase());
            if (col != null) {
                Cell cell = row.getCell(col);
                if (cell != null) setFieldValue(f, obj, cell);
            }
        }
        return obj;
    }

    private void setFieldValue(Field field, T obj, Cell cell) throws IllegalAccessException {
        Class<?> type = field.getType();
        switch (cell.getCellType()) {
            case STRING -> {
                String val = cell.getStringCellValue();
                if (type == String.class) field.set(obj, val);
                else if (type == int.class || type == Integer.class) field.set(obj, Integer.parseInt(val));
                else if (type == long.class || type == Long.class) field.set(obj, Long.parseLong(val));
                else if (type == double.class || type == Double.class) field.set(obj, Double.parseDouble(val));
                else if (type == boolean.class || type == Boolean.class) field.set(obj, Boolean.parseBoolean(val));
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    if (type == Date.class) field.set(obj, d);
                    else if (type == LocalDate.class)
                        field.set(obj, d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    else if (type == LocalDateTime.class)
                        field.set(obj, d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                } else {
                    double num = cell.getNumericCellValue();
                    if (type == int.class || type == Integer.class) field.set(obj, (int) num);
                    else if (type == long.class || type == Long.class) field.set(obj, (long) num);
                    else if (type == double.class || type == Double.class) field.set(obj, num);
                    else if (type == String.class) field.set(obj, String.valueOf(num));
                }
            }
            case BOOLEAN -> {
                if (type == boolean.class || type == Boolean.class) field.set(obj, cell.getBooleanCellValue());
            }
            default -> {
            }
        }
    }

    private void writeHeader(Row row) {
        int col = 0;
        for (Field f : fields) row.createCell(col++).setCellValue(f.getName());
    }

    private int writeData(Sheet sheet, int startRow, List<T> data) throws IllegalAccessException {
        int rowNum = startRow;
        for (T obj : data) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            for (Field f : fields) {
                Cell cell = row.createCell(col++);
                Object val = f.get(obj);
                setCellValue(cell, val);
            }
        }
        return rowNum;
    }

    private void setCellValue(Cell cell, Object val) {
        if (val == null) return;
        if (val instanceof String s) cell.setCellValue(s);
        else if (val instanceof Integer i) cell.setCellValue(i);
        else if (val instanceof Long l) cell.setCellValue(l);
        else if (val instanceof Double d) cell.setCellValue(d);
        else if (val instanceof Boolean b) cell.setCellValue(b);
        else if (val instanceof Date d) cell.setCellValue(d);
        else if (val instanceof LocalDate ld) cell.setCellValue(ld.toString());
        else if (val instanceof LocalDateTime ldt) cell.setCellValue(ldt.toString());
        else cell.setCellValue(val.toString());
    }
}
