package auto.framework.utils;

import auto.framework.helpers.RuleConfigHelper;
import auto.framework.models.dto.DataCSV;
import auto.framework.models.enums.DbType;
import auto.framework.helpers.DbMetaHelper;
import auto.framework.models.enums.FileConfig;

import java.sql.*;
import java.util.*;


public class DbUtil {

    /**
     * CONNECTION
     */
    public static Connection oracle(String url, String user, String pass) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(url, user, pass);
    }

    public static Connection postgres(String url, String user, String pass) throws Exception {
        Class.forName("org.postgresql.Driver");
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

    /**
     * CORE HASH QUERY
     */
//    public static Map<String, String> queryToHashMap(
//            Connection conn,
//            String table,
//            String pkColumns
//    ) throws Exception {
//
//        DbType dbType = DbType.from(conn);
//        RuleConfigHelper.loadRules();
//
//        List<String> pkList = DbMetaHelper.resolvePk(conn, table, pkColumns);
//        List<String> normalized = DbMetaHelper.detectNormalizedColumns(conn, table, dbType, pkList);
//
//        String sql = buildHashQuery(table, pkList, normalized, dbType);
//
//        Map<String, String> result = new HashMap<>();
//        List<DataCSV> csvData = new ArrayList<>();
//
//        try (PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setFetchSize(dbType == DbType.ORACLE ? 5_000 : 10_000);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) {
//                    String id = DbMetaHelper.buildCompositeKey(rs, pkList);
//                    String hash = normalized.isEmpty() ? "null" : rs.getString("h").toLowerCase();
//                    String expr = normalized.isEmpty() ? "null" : rs.getString("expr_debug");
//
//                    result.put(id, hash);
//                    csvData.add(new DataCSV(id, hash, expr));
//                }
//            }
//        }
//
//        new CsvUtil<>(DataCSV.class)
//                .write("log/" + dbType.name().toLowerCase() + "_" + table + ".csv", csvData);
//
//        return result;
//    }
    public static Map<String, String> queryToHashMap(
            Connection conn,
            String table,
            String pkColumns
    ) throws Exception {

        DbType dbType = DbType.from(conn);
        RuleConfigHelper.loadRules();

        List<String> pkList = DbMetaHelper.resolvePk(conn, table, pkColumns);

        List<String> normalized = DbMetaHelper.detectNormalizedColumns(conn, table, dbType, pkList);

        String sql = buildHashQuery(table, pkList, normalized, dbType);

        Map<String, String> result = new HashMap<>();
        String dataFile = FileConfig.REPORT_PATH.replace("{path}", table.toUpperCase() + "_" + dbType.name() + ".csv");

        try (
                CsvStreamWriter<DataCSV> csv = new CsvStreamWriter<>(dataFile, DataCSV.class, ',', 10000);
                PreparedStatement ps = conn.prepareStatement(
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
                    csv.write(new DataCSV(id, hash, expr));
                }
            }
        }

        return result;
    }


    public static int queryCount(Connection conn, String table) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM " + table;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        LogUtil.error("COUNT query returned no result for table " + table);
        return -1;
    }

    public static List<String> queryDuplicateKeys(
            Connection conn,
            String table,
            String pkColumns
    ) throws Exception {
        List<String> pkList = pkColumns.equalsIgnoreCase("null")
                ? DbMetaHelper.getPrimaryKeyColumns(conn, table)
                : DbMetaHelper.parsePk(pkColumns);

        String pks = String.join(", ", pkList);
        String sql = String.format(
                "SELECT %s, COUNT(*) AS cnt FROM %s GROUP BY %s HAVING COUNT(*) > 1",
                pks, table, pks
        );
        List<String> duplicates = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                duplicates.add(DbMetaHelper.buildCompositeKey(rs, pkList));
            }
        }

        return duplicates;
    }


}

