package auto.framework.models.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyMatchingResult {
    private String tableName;
    private int totalOracle;
    private int totalPostgres;
    private int missingInOracle;
    private int missingInPostgres;
    private int mismatch;
}
