package auto.framework.models.result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyMatchingResult {
    private String tableName;
    private int totalSource;
    private int totalTarget;
    private int missingInSource;
    private int missingInTarget;
    private int mismatch;
}
