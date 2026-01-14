package auto.framework;

import auto.framework.models.entity.Person;
import auto.framework.utils.ExcelUtil;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        ExcelUtil<Person> excelUtil = new ExcelUtil<>(Person.class);

        String personFile_read = "src/main/resources/Person.xlsx";
        String personFile_write = "src/main/resources/Person_2.xlsx";
        try {
//            // 3. Đọc file Excel (no batch)
//            List<Person> readPeople = excelUtil.read(personFile_read);
//            readPeople.forEach(System.out::println);
//
//            // 2. Ghi file Excel (no batch)
//            excelUtil.write(personFile_write, readPeople);
//            System.out.println("Write no batch done!");


//            // 4. Đọc file theo batch (batchSize = 20)
//            List<Person> readPeople = new ArrayList<>();
//            System.out.println("\nRead batch (batch size 20):");
//            excelUtil.readBatch(personFile_read, 20, batch -> {
//                System.out.println("Batch size = " + batch.size());
//                batch.stream().limit(3).forEach(System.out::println); // in 3 dòng đầu mỗi batch
//                for (Person person : batch) {
//                    readPeople.add(person);
//                }
//            });
//
//            // 5. Ghi file theo batch
//            // Chia list thành batches 25 để test
//            List<List<Person>> batches = new ArrayList<>();
//            int batchSize = 25;
//            for (int i = 0; i < readPeople.size(); i += batchSize) {
//                batches.add(readPeople.subList(i, Math.min(i + batchSize, readPeople.size())));
//            }
//
//            excelUtil.writeBatch(personFile_write, batches);
//            System.out.println("\nWrite batch done!");


            excelUtil.writeBatch(personFile_write, batchWriter -> {
                try {
                    excelUtil.readBatch(personFile_read, 20, batch -> {
                        // Xử lý nếu muốn (in 3 dòng đầu mỗi batch)
                        batch.stream().limit(3).forEach(System.out::println);
                        // Ghi ngay batch vào file
                        batchWriter.accept(batch);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}