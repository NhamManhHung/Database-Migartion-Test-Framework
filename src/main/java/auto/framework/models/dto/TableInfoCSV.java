package auto.framework.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoCSV {
    String tcKeyCount;
    String tcKeyDuplicate;
    String tcKeyMatching;
    String tableName;
    String primaryKeyColumn;
}