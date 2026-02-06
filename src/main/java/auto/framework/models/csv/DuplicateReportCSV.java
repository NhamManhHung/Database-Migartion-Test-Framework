package auto.framework.models.csv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateReportCSV {
    String primaryKeyValue;
    int count;
}
