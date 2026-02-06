package auto.framework.models.connection;

import auto.framework.models.enums.DbRole;
import auto.framework.models.enums.DbType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Connection;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionData {
    private Connection connection;
    private DbRole dbRole;
}
