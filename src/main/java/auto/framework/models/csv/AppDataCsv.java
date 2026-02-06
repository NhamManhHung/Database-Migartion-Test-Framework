package auto.framework.models.csv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppDataCsv {
    String tcKeyCount;
    String tcKeyDuplicate;
    String tcKeyMatching;
    String tableName;
    String primaryKeyColumn;
}