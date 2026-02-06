package auto.framework;

import auto.framework.models.csv.AppDataCsv;
import auto.framework.utils.CsvUtil;

import java.util.*;

public class MainDB {
    public static void main(String[] args) throws Exception {


//        Connection oracle = DbUtil.getOracleConnection(
//                ConfigUtil.get("oracle.url"),
//                ConfigUtil.get("oracle.user"),
//                ConfigUtil.get("oracle.pass")
//        );
//
//        Connection postgres = DbUtil.getPostgresConnection(
//                ConfigUtil.get("postgres.url"),
//                ConfigUtil.get("postgres.user"),
//                ConfigUtil.get("postgres.pass")
//        );
//
//
//        Map<String, String> type1Oracle =
//                DbUtil.queryToHashMap(oracle, "test_types1", "id");
//
//        Map<String, String> type1Postgres =
//                DbUtil.queryToHashMap(postgres, "test_types1", "id");
//
//        System.out.println("=== Compare test_types1 ===");
//        DbUtil.compareHash(type1Oracle, type1Postgres, "test_types1");


        CsvUtil<AppDataCsv> csv = new CsvUtil<>(AppDataCsv.class);
        List<AppDataCsv> list = csv.read("");
        for (AppDataCsv table : list) {
            System.out.println(table);
        }

    }
}
