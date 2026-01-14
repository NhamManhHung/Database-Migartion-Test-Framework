package auto.framework;

import auto.framework.models.entity.Person;
import auto.framework.utils.CsvUtil;

import java.util.List;

public class MainCsv {
    public static void main(String[] args) {
        CsvUtil<Person> csvUtil = new CsvUtil<>(Person.class);

        String personFile_read = "src/main/resources/person.csv";
        String personFile_write = "src/main/resources/person_2.csv";
        String diffFile = "src/main/resources/diff.csv";
        try {
//            // 3. Đọc file Excel (no batch)
//            List<Person> readPeople = csvUtil.read(personFile_read);
//            readPeople.forEach(System.out::println);
//
//            // 2. Ghi file Excel (no batch)
//            csvUtil.write(personFile_write, readPeople);
//            System.out.println("Write no batch done!");
//
//
//            // 4. Đọc file theo batch (batchSize = 20)
//            List<Person> readPeople = new ArrayList<>();
//            System.out.println("\nRead batch (batch size 20):");
//            csvUtil.readBatch(personFile_read, 20, batch -> {
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
//            csvUtil.writeBatch(personFile_write, batches);
//            System.out.println("\nWrite batch done!");

//            System.out.println("\nRead & Write batch streaming (batch size 20 -> write batch size 25):");
//
//            csvUtil.writeBatch(personFile_write, batchWriter -> {
//                // Móc nối trực tiếp: đọc batch từ CSV và đẩy vào writer
//                try {
//                    csvUtil.readBatch(personFile_read, 20, batch -> {
//                        // Xử lý nếu muốn (in 3 dòng đầu mỗi batch)
//                        batch.stream().limit(3).forEach(System.out::println);
//
//                        // Đẩy batch vào writer ngay
//                        batchWriter.accept(batch);
//                    });
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//            System.out.println("\nStreaming Write batch done!");
//
//            List<Person> persons_1 = csvUtil.read(personFile_read);
//            List<Person> persons_2 = csvUtil.read(personFile_write);
//
//            List<Person> diff = csvUtil.compareList(persons_1, persons_2);
//            csvUtil.write(diffFile, diff);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
