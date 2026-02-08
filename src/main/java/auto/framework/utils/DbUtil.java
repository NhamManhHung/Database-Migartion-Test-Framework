package auto.framework.utils;

import auto.framework.helpers.RuleConfigHelper;
import auto.framework.models.connection.ConnectionData;
import auto.framework.models.csv.DuplicateReportCSV;
import auto.framework.models.csv.TableDataCsv;
import auto.framework.models.csv.TableDataCsv;
import auto.framework.models.enums.DbType;
import auto.framework.helpers.DbMetaHelper;
import auto.framework.models.enums.FileConfig;

import java.sql.*;
import java.util.*;


public class DbUtil {

    /**
     * CONNECTION
     */

    public static Connection connectDb(String url, String user, String pass, String className) throws Exception {
        Class.forName(className);
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * SQL QUERY BUILDER
     */
    private static String buildHashQuery(
            String table,
            List<String> pkList,
            List<String> normalized,
            DbType dbType
    ) {
        String pkCols = String.join(", ", pkList);

        if (normalized.isEmpty()) {
            return "SELECT " + pkCols + " FROM " + table;
        }

        String joinExpr = String.join(" || '|' || ", normalized);
        String hashExpr = dbType == DbType.ORACLE
                ? "STANDARD_HASH(" + joinExpr + ", 'SHA256')"
                : "encode(sha256(convert_to(" + joinExpr + ", 'UTF8')), 'hex')";

        return String.format(
                "SELECT %s, lower(%s) AS h, (%s) AS expr_debug FROM %s",
                pkCols, hashExpr, joinExpr, table
        );
    }

    public static Map<String, String> queryToHashMap(
            ConnectionData conn,
            String table,
            String pkColumns
    ) throws Exception {

        DbType dbType = DbType.from(conn.getConnection());
        RuleConfigHelper.loadRules();

        List<String> pkList = DbMetaHelper.resolvePk(conn.getConnection(), table, pkColumns);

        List<String> normalized = DbMetaHelper.detectNormalizedColumns(conn.getConnection(), table, dbType, pkList);

        String sql = buildHashQuery(table, pkList, normalized, dbType);

        Map<String, String> result = new HashMap<>();
        String dataFile = FileConfig.TABLE_DATA_REPORT
                .replace("{table}", table.toUpperCase())
                .replace("{role}", conn.getDbRole().toString().toUpperCase());

        try (
                CsvStreamWriter<TableDataCsv> csv = new CsvStreamWriter<>(dataFile, TableDataCsv.class, ',', 10000);
                PreparedStatement ps = conn.getConnection().prepareStatement(
                        sql,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY
                )
        ) {
            ps.setFetchSize(dbType == DbType.ORACLE ? 5_000 : 10_000);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = DbMetaHelper.buildCompositeKey(rs, pkList);
                    String hash = normalized.isEmpty() ? "null" : rs.getString("h");
                    String expr = normalized.isEmpty() ? "null" : rs.getString("expr_debug");

                    result.put(id, hash);
                    csv.write(new TableDataCsv(id, hash, expr));
                }
            }
        }
        return result;
    }


    public static int queryCount(ConnectionData conn, String table) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM " + table;

        try (PreparedStatement ps = conn.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        LogUtil.error("COUNT query returned no result for table " + table);
        return -1;
    }

    public static List<String> queryDuplicateKeys(
            ConnectionData conn,
            String table,
            String pkColumns
    ) throws Exception {
        DbType dbType = DbType.from(conn.getConnection());
        List<String> pkList = pkColumns.equalsIgnoreCase("null")
                ? DbMetaHelper.getPrimaryKeyColumns(conn.getConnection(), table)
                : DbMetaHelper.parsePk(pkColumns);

        String pks = String.join(", ", pkList);
        String sql = String.format(
                "SELECT %s, COUNT(*) AS cnt FROM %s GROUP BY %s HAVING COUNT(*) > 1",
                pks, table, pks
        );
        List<String> duplicates = new ArrayList<>();

        String dataFile = FileConfig.DUPLICATE_REPORT
                .replace("{table}", table.toUpperCase())
                .replace("{role}", conn.getDbRole().toString().toUpperCase());

        try (
                CsvStreamWriter<DuplicateReportCSV> csv = new CsvStreamWriter<>(dataFile, DuplicateReportCSV.class, ',', 10000);
                PreparedStatement ps = conn.getConnection().prepareStatement(
                        sql,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY
                )
        ) {
            ps.setFetchSize(dbType == DbType.ORACLE ? 5_000 : 10_000);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = DbMetaHelper.buildCompositeKey(rs, pkList);
                    int count = rs.getInt("cnt");
                    csv.write(new DuplicateReportCSV(id, count));
                    duplicates.add(id);
                }

            }
        }

        return duplicates;
    }

    public static List<String> getSourceSchemaTables(ConnectionData conn) throws SQLException {
        List<String> tables = new ArrayList<>();

        String sql =
                "SELECT t.owner, t.table_name " +
                        "FROM all_tables t " +
                        "JOIN all_users u ON t.owner = u.username " +
                        "WHERE t.table_name NOT LIKE 'BIN$%' " +
                        "AND u.oracle_maintained = 'N'";

        try (PreparedStatement stmt = conn.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String schema = rs.getString("owner");
                String table = rs.getString("table_name");

                tables.add(schema + "." + table);
            }
        }

        return tables;
    }


}

