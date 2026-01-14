package auto.framework.models.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateResult {
    private String tableName;
    private int duplicateInOracle;
    private int duplicateInPostgres;
}
