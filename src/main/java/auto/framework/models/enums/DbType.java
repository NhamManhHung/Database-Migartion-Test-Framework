package auto.framework.models.enums;

import java.sql.Connection;
import java.sql.SQLException;

public enum DbType {
    ORACLE,
    POSTGRES;

    public static DbType from(Connection conn) throws SQLException {
        String name = conn.getMetaData()
                .getDatabaseProductName()
                .toLowerCase();
        return name.contains("oracle") ? ORACLE : POSTGRES;
    }
}
