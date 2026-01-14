package auto.framework.helpers;

import auto.framework.models.enums.DbType;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DbMetaHelper {

    private static final Map<String, List<String>> PK_CACHE = new ConcurrentHashMap<>();

    /**
     * METADATA
     */
    public static List<String> resolvePk(
            Connection conn,
            String table,
            String pkColumns
    ) throws SQLException {

        if (pkColumns != null && !pkColumns.equalsIgnoreCase("null")) {
            return parsePk(pkColumns);
        }

        List<String> pkList = PK_CACHE.computeIfAbsent(
                conn.getMetaData().getURL() + "|" + table,
                k -> {
                    try {
                        return getPrimaryKeyColumns(conn, table);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        PrimaryKeyExportHelper.registerIfAuto(table, pkList);

        return pkList;
    }

    public static List<String> detectNormalizedColumns(
            Connection conn,
            String table,
            DbType dbType,
            List<String> pkList
    ) throws Exception {

        String sql = dbType == DbType.ORACLE
                ? "SELECT * FROM " + table + " WHERE ROWNUM = 1"
                : "SELECT * FROM " + table + " LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return NormalizeExpressHelper.buildNormalizeExpressions(
                    rs.getMetaData(), dbType.name().toLowerCase(), pkList
            );
        }
    }

    /**
     * HELPER
     */
    public static List<String> parsePk(String value) {
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String buildCompositeKey(
            ResultSet rs,
            List<String> pkColumns
    ) throws SQLException {

        StringJoiner joiner = new StringJoiner("|");
        for (String pk : pkColumns) {
            joiner.add(
                    Optional.ofNullable(rs.getString(pk)).orElse("NULL")
            );
        }
        return joiner.toString();
    }

    public static List<String> getPrimaryKeyColumns(Connection conn, String table)
            throws SQLException {

        DatabaseMetaData meta = conn.getMetaData();
        String dbName = meta.getDatabaseProductName().toLowerCase();

        String schema = null;
        String tableName = table;

        if (table.contains(".")) {
            String[] parts = table.split("\\.", 2);
            schema = parts[0];
            tableName = parts[1];
        }

        if (dbName.contains("oracle")) {

            if (schema == null) {
                schema = meta.getUserName();
            }
            schema = schema.toUpperCase();
            tableName = tableName.toUpperCase();

        } else if (dbName.contains("postgresql")) {

            if (schema == null) {
                schema = "public";
            }

            schema = schema.replace("\"", "");
            tableName = tableName.replace("\"", "");

            schema = schema.toLowerCase();
            tableName = tableName.toLowerCase();
        }

        Map<Short, String> pkMap = new TreeMap<>();

        try (ResultSet rs = meta.getPrimaryKeys(null, schema, tableName)) {
            while (rs.next()) {
                pkMap.put(
                        rs.getShort("KEY_SEQ"),
                        rs.getString("COLUMN_NAME")
                );
            }
        }

        return new ArrayList<>(pkMap.values());
    }
}