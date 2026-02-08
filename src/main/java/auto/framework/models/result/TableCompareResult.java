package auto.framework.models.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableCompareResult {
    Set<String> sourceDataSet;
    Set<String> csvDataSet;
}
