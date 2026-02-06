package auto.framework.models.connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefectRequest {

    private String summary;
    private String description;
    private String epicLink;
}
