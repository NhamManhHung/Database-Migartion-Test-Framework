package tests.common;

import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.integration.IJiraService;
import auto.framework.integration.IZephyrService;
import auto.framework.integration.IntegrationProvider;
import auto.framework.models.connection.ConnectionData;
import auto.framework.models.csv.AppDataCsv;
import auto.framework.models.enums.DbRole;
import auto.framework.models.enums.DbType;
import auto.framework.models.enums.FileConfig;
import auto.framework.models.enums.TestcaseType;
import auto.framework.helpers.DefectBuilderHelper;
import auto.framework.models.connection.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import auto.framework.utils.DbUtil;
import auto.framework.utils.LogUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseTest {

    protected static ConnectionData sourceDb;
    protected static ConnectionData targetDb;
    protected static List<AppDataCsv> tables;

    protected static boolean cycleIsExist;
    protected static String runKey;
    protected static IZephyrService zephyr;
    protected static IJiraService jira;

    /* ===================== BEFORE SUITE ===================== */

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception {

        CsvUtil<AppDataCsv> csv = new CsvUtil<>(AppDataCsv.class);
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(FileConfig.ALL_TABLE_DATA_PATH.replace("{app}", ConfigUtil.getEnv("app.name")))) {

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

    @BeforeTest(alwaysRun = true)
    public void setup() throws Exception {
        Connection sourceConn = createConnection("source");
        Connection targetConn = createConnection("target");

        sourceDb = new ConnectionData(sourceConn, DbRole.SOURCE);
        targetDb = new ConnectionData(targetConn, DbRole.TARGET);
    }

    /* ===================== AFTER SUITE ===================== */

    @AfterTest(alwaysRun = true)
    public void tearDown() throws Exception {
        if (sourceDb.getConnection() != null) sourceDb.getConnection().close();
        if (targetDb.getConnection() != null) targetDb.getConnection().close();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {

        Map<String, List<String>> detectedPk =
                PrimaryKeyExportHelper.getAll();

        String dataFile = FileConfig.ALL_TABLE_DATA_REPORT
                .replace("{app}", ConfigUtil.getEnv("app.name"));

        CsvUtil.exportResolvedPkCsv(
                dataFile,
                tables,
                detectedPk
        );
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        System.out.println("cycleIsExist: " + cycleIsExist);

        AppDataCsv table = (AppDataCsv) result.getParameters()[0];
        String testType = result.getMethod().getDescription();

        String testCaseKey = getTestCaseKey(table, testType);
        List<String> filePaths = getFilePaths(table, testType);

        if (result.getStatus() == ITestResult.FAILURE) {
            handleTestFailure(result, testType, table, testCaseKey, filePaths);
        } else {
            reportTestSuccess(testCaseKey);
        }
    }

    private String getTestCaseKey(AppDataCsv table, String testType) {
        return switch (testType) {
            case TestcaseType.COUNT -> table.getTcKeyCount();
            case TestcaseType.DUPLICATE -> table.getTcKeyDuplicate();
            case TestcaseType.KEY_MATCHING -> table.getTcKeyMatching();
            default -> throw new RuntimeException("Unknown test case type: " + testType);
        };
    }

    private List<String> getFilePaths(AppDataCsv table, String testType) {
        List<String> filePaths = new ArrayList<>();

        switch (testType) {
            case TestcaseType.DUPLICATE:
                filePaths.add(
                        FileConfig.DUPLICATE_REPORT
                                .replace("{table}", table.getTableName().toUpperCase())
                                .replace("{role}", sourceDb.getDbRole().toString().toUpperCase()));
                filePaths.add(
                        FileConfig.DUPLICATE_REPORT
                                .replace("{table}", table.getTableName().toUpperCase())
                                .replace("{role}", targetDb.getDbRole().toString().toUpperCase()));
                break;
            case TestcaseType.KEY_MATCHING:
                filePaths.add(FileConfig.COMPARE_REPORT.replace("{table}", table.getTableName().toUpperCase()));
//                filePaths.add(FileConfig.TABLE_DATA_REPORT.replace("{table}", table.getTableName().toUpperCase()).replace("{role}", sourceDb.getDbRole().toString().toUpperCase()));
//                filePaths.add(FileConfig.TABLE_DATA_REPORT.replace("{table}", table.getTableName().toUpperCase()).replace("{role}", targetDb.getDbRole().toString().toUpperCase()));
                break;
        }

        return filePaths;
    }

    private void handleTestFailure(ITestResult result, String testType, AppDataCsv table,
                                   String testCaseKey, List<String> filePaths) {
        DefectRequest req = DefectBuilderHelper.build(
                testType,
                table.getTableName(),
                result.getAttribute(TestcaseType.DETAIL_DATA),
                ConfigUtil.getEnv("zephyr.epic")
        );

        //Handle jira defect and attachments
        String defectKey = jira.createDefect(req);
        jira.attachFiles(defectKey, filePaths, "file");


        //Handle zephyr test result and attachments
        String testResultId = zephyr.reportResult(runKey, testCaseKey, "Fail", defectKey, req);
        List<String> fileList = new ArrayList<>();
        fileList.add(FileConfig.COMPARE_REPORT.replace("{table}", table.getTableName().toUpperCase()));
        fileList.add(FileConfig.TABLE_DATA_REPORT.replace("{table}", table.getTableName().toUpperCase()).replace("{role}", sourceDb.getDbRole().toString().toUpperCase()));
        fileList.add(FileConfig.TABLE_DATA_REPORT.replace("{table}", table.getTableName().toUpperCase()).replace("{role}", targetDb.getDbRole().toString().toUpperCase()));
        if (testResultId != null && !testResultId.isEmpty()) {
            zephyr.attachFiles(runKey, testCaseKey, testResultId, fileList, "file");
        } else {
            LogUtil.error("Cannot attach files to Zephyr: testResultId is null");
        }
    }

    private void reportTestSuccess(String testCaseKey) {
        zephyr.reportResult(runKey, testCaseKey, "Pass", null, null);
    }

    private Connection createConnection(String prefix) throws Exception {
        String type = ConfigUtil.getEnv(prefix + ".type");
        String url = ConfigUtil.getEnv(prefix + ".url");
        String user = ConfigUtil.getEnv(prefix + ".user");
        String pass = ConfigUtil.getEnv(prefix + ".pass");

        return switch (type.toUpperCase()) {
            case "ORACLE" ->
                    DbUtil.connectDb(url, user, pass, ConfigUtil.get("oracle.driver", FileConfig.APPLICATION_FILE));
            case "POSTGRES" ->
                    DbUtil.connectDb(url, user, pass, ConfigUtil.get("postgres.driver", FileConfig.APPLICATION_FILE));
            default -> {
                System.out.println("Unknown database type: " + type);
                yield null;
            }
        };
    }

}
