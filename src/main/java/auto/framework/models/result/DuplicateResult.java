package auto.framework.models.result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateResult {
    private String tableName;
    private int duplicateInSource;
    private int duplicateInTarget;
}
