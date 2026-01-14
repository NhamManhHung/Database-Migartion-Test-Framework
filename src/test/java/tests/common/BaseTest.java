package tests.common;

import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.models.dto.TableInfoCSV;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import auto.framework.utils.DbUtil;
import org.testng.annotations.*;
import tests.testdata.MigrationDataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class BaseTest {
    private static final String CSV_FILE = "file/csv.properties";
    protected static Connection oracle;
    protected static Connection postgres;
    protected static List<TableInfoCSV> tables;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception {
        CsvUtil<TableInfoCSV> csv = new CsvUtil<>(TableInfoCSV.class);
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("data/MBF_CDS.csv")) {
            tables = csv.read(is);
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        Map<String, List<String>> detectedPk =
                PrimaryKeyExportHelper.getAll();
        String dataFile = ConfigUtil.get(CSV_FILE, "report.path").replace("{path}", "MBF_CDS.csv");
        CsvUtil.exportResolvedPkCsv(
                dataFile,
                tables,
                detectedPk
        );
    }

    @BeforeTest
    public void setup() throws Exception {
        oracle = DbUtil.oracle(
                ConfigUtil.getEnv("oracle.url"),
                ConfigUtil.getEnv("oracle.user"),
                ConfigUtil.getEnv("oracle.pass")
        );

        postgres = DbUtil.postgres(
                ConfigUtil.getEnv("postgres.url"),
                ConfigUtil.getEnv("postgres.user"),
                ConfigUtil.getEnv("postgres.pass")
        );
    }

    @AfterTest
    public void tearDown() throws Exception {
        if (oracle != null) oracle.close();
        if (postgres != null) postgres.close();
    }
}
