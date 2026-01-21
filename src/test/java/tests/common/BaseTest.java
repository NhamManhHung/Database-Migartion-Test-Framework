package tests.common;

import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.integration.IJiraService;
import auto.framework.integration.IntegrationProvider;
import auto.framework.integration.IZephyrService;
import auto.framework.models.dto.TableInfoCSV;
import auto.framework.models.enums.FileConfig;
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
        runKey = ConfigUtil.getEnv("runKey");

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
        Object[] params = result.getParameters();

        String testType = result.getMethod().getDescription();
        TableInfoCSV table = (TableInfoCSV) params[0];
        String testCaseKey = switch (testType) {
            case TestcaseType.COUNT -> table.getTcKeyCount();
            case TestcaseType.DUPLICATE -> table.getTcKeyDuplicate();
            case TestcaseType.KEY_MATCHING -> table.getTcKeyMatching();
            default -> throw new RuntimeException("Unknown test case type: " + testType);
        };

        if (result.getStatus() == ITestResult.FAILURE) {
            DefectRequest req = DefectBuilder.build(
                    testType,
                    table.getTableName(),
                    result.getAttribute(TestcaseType.DETAIL_DATA),
                    ConfigUtil.getEnv("zephyr.epic")
            );

            String defectKey = jira.createDefect(req);

            zephyr.reportResult(
                    runKey,
                    testCaseKey,
                    "Fail",
                    defectKey,
                    req
            );

        } else {

            zephyr.reportResult(
                    runKey,
                    testCaseKey,
                    "Pass",
                    null,
                    null
            );
        }
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {

        Map<String, List<String>> detectedPk =
                PrimaryKeyExportHelper.getAll();

        String dataFile = FileConfig.REPORT_PATH
                .replace("{path}", "MBF_CDS.csv");

        CsvUtil.exportResolvedPkCsv(
                dataFile,
                tables,
                detectedPk
        );
    }
}
