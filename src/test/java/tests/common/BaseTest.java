package tests.common;

import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.integration.IJiraService;
import auto.framework.integration.IntegrationProvider;
import auto.framework.integration.IZephyrService;
import auto.framework.models.dto.CountResult;
import auto.framework.models.dto.TableInfoCSV;
import auto.framework.models.enums.TestcaseType;
import auto.framework.reporting.DefectBuilder;
import auto.framework.reporting.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import auto.framework.utils.DbUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class BaseTest {

    private static final String CSV_FILE = "file/csv.properties";

    protected static Connection oracle;
    protected static Connection postgres;
    protected static List<TableInfoCSV> tables;

    protected static boolean cycleIsExist;
    protected static String runKey;
    protected static IZephyrService zephyr;
    protected static IJiraService jira;

    /* ===================== BEFORE SUITE ===================== */

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception {

        CsvUtil<TableInfoCSV> csv = new CsvUtil<>(TableInfoCSV.class);
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("data/MBF_CDS.csv")) {

            tables = csv.read(is);
        }

        zephyr = IntegrationProvider.zephyr();
        jira = IntegrationProvider.jira();
        runKey = ConfigUtil.getEnv("zephyr.runKey");

        try {
            cycleIsExist = zephyr.isTestRunExist(runKey);
        } catch (Exception e) {
            cycleIsExist = false;
            System.out.println("[WARN] Cannot connect Zephyr: " + e.getMessage());
        }
        System.out.println("Cycling isExist: " + cycleIsExist);

        if (!cycleIsExist) {
            throw new RuntimeException(
                    "[FATAL] Zephyr Test Cycle " + runKey + " is not existed. " +
                            "Cannot proceed with tests."
            );
        }
    }

    /* ===================== DB SETUP ===================== */

    @BeforeTest(alwaysRun = true)
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

    @AfterTest(alwaysRun = true)
    public void tearDown() throws Exception {
        if (oracle != null) oracle.close();
        if (postgres != null) postgres.close();
    }

    /* ===================== AFTER SUITE ===================== */

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        System.out.println("cycleIsExist: " + cycleIsExist);

        TableInfoCSV tableInfoCSV = (TableInfoCSV) result.getAttribute("TABLE_INFO");
        String testCaseKey = tableInfoCSV.getTcKeyCount();
        if (result.getStatus() == ITestResult.FAILURE) {

            TestcaseType type =
                    (TestcaseType) result.getAttribute("COMPARE_TYPE");

            if (type == null) {
                type = TestcaseType.KEY_MATCHING;
            }

            CountResult detail =
                    (CountResult) result.getAttribute("COMPARE_DETAIL");

            DefectRequest req = DefectBuilder.build(
                    type,
                    tableInfoCSV.getTableName(),
                    detail,
                    ConfigUtil.getEnv("zephyr.epic.cds")
            );

            String defectKey = jira.createDefect(req);

            zephyr.reportResult(
                    runKey,
                    testCaseKey,
                    "Fail",
                    defectKey
            );

        } else {

            zephyr.reportResult(
                    runKey,
                    testCaseKey,
                    "Pass",
                    null
            );
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {

        Map<String, List<String>> detectedPk =
                PrimaryKeyExportHelper.getAll();

        String dataFile = ConfigUtil
                .get(CSV_FILE, "report.path")
                .replace("{path}", "MBF_CDS.csv");

        CsvUtil.exportResolvedPkCsv(
                dataFile,
                tables,
                detectedPk
        );
    }
}
