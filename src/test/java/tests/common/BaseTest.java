package tests.common;

import auto.framework.helpers.DataCsvHelper;
import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.integration.IJiraService;
import auto.framework.integration.IZephyrService;
import auto.framework.integration.IntegrationProvider;
import auto.framework.models.connection.ConnectionData;
import auto.framework.models.csv.AppDataCsv;
import auto.framework.models.enums.DbRole;
import auto.framework.models.enums.FileConfig;
import auto.framework.models.enums.TestcaseType;
import auto.framework.helpers.DefectBuilderHelper;
import auto.framework.models.connection.DefectRequest;
import auto.framework.models.result.TableCompareResult;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import auto.framework.utils.DbUtil;
import auto.framework.utils.LogUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;
import tests.listener.TestListener;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class BaseTest {

    protected static ConnectionData sourceDb;
    protected static ConnectionData targetDb;
    protected static List<AppDataCsv> tables;

    protected static boolean isCycleExist;
    protected static boolean hasRunKey;
    protected static String runKey;
    protected static IZephyrService zephyr;
    protected static IJiraService jira;
    private static String defectKeyTableDataCompare;
    protected static Set<String> sourceDataSet;
    protected static Set<String> csvDataSet;
    protected static Set<String> missingInCsv;
    protected static Set<String> missingInSource;

    /* ===================== BEFORE SUITE ===================== */

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception {

        tables = DataCsvHelper.getTables();
        System.out.println(tables);
        runKey = ConfigUtil.getEnv("runKey");

        hasRunKey = runKey != null;

        if (!hasRunKey) {
            LogUtil.info("Running without runKey");
            return;
        }

        zephyr = IntegrationProvider.zephyr();
        jira = IntegrationProvider.jira();
        validateZephyrConnection(runKey);
        setup();
        compareTableData();
        createAppTableDataReport();
    }

    /* ===================== AFTER SUITE ===================== */
    @AfterSuite(alwaysRun = true)
    private void tearDown() throws Exception {
        if (sourceDb.getConnection() != null) sourceDb.getConnection().close();
        if (targetDb.getConnection() != null) targetDb.getConnection().close();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        if (!hasRunKey) return;
        int status = result.getStatus();

        AppDataCsv table = null;
        Object[] params = result.getParameters();

        if (params != null && params[0] instanceof AppDataCsv) {
            table = (AppDataCsv) params[0];
        }


        String testType = result.getMethod().getDescription();
        String testCaseKey = getTestCaseKey(table, testType);

        DefectRequest req = DefectBuilderHelper.build(
                testType,
                table != null ? table.getTableName() : "",
                result.getAttribute(TestcaseType.DETAIL_DATA),
                ConfigUtil.getEnv("zephyr.epic")
        );
        handleIndividualTestCase(result, status, testCaseKey, table, req, testType);

    }

    private void compareTableData() throws SQLException {
        List<String> sourceData = DbUtil.getSourceSchemaTables(sourceDb);
        sourceDataSet = sourceData.stream()
                .map(name -> name.trim().toUpperCase())
                .collect(Collectors.toSet());

        csvDataSet = tables.stream()
                .map(csv -> csv.getTableName().trim().toUpperCase())
                .collect(Collectors.toSet());

        missingInCsv = new HashSet<>(sourceDataSet);
        missingInCsv.removeAll(csvDataSet);

        missingInSource = new HashSet<>(csvDataSet);
        missingInSource.removeAll(sourceDataSet);
        TestListener.logTableCompareResult(new TableCompareResult(missingInSource, missingInCsv));
        if (!missingInSource.isEmpty() && hasRunKey) {
            DefectRequest req = DefectBuilderHelper.build(
                    null,
                    null,
                    null,
                    ConfigUtil.getEnv("zephyr.epic")
            );
            defectKeyTableDataCompare = jira.createDefect(req);
        }
    }

    private void setup() throws Exception {
        Connection sourceConn = createConnection("source");
        Connection targetConn = createConnection("target");

        sourceDb = new ConnectionData(sourceConn, DbRole.SOURCE);
        targetDb = new ConnectionData(targetConn, DbRole.TARGET);
    }

    private void createAppTableDataReport() {
        Map<String, List<String>> detectedPk =
                PrimaryKeyExportHelper.getAll();

        String dataFile = FileConfig.ALL_TABLE_DATA_REPORT
                .replace("{app}", ConfigUtil.getEnv("app.name"));

        List<AppDataCsv> listData = new ArrayList<>(tables);
        missingInCsv.forEach(tableName ->
                listData.add(new AppDataCsv("null", "null", "null", tableName, "null"))
        );

        CsvUtil.exportResolvedPkCsv(dataFile, listData, detectedPk);
    }

    private void handleIndividualTestCase(ITestResult result, int status, String testCaseKey, AppDataCsv table, DefectRequest req, String testType) {
        switch (status) {
            case ITestResult.SUCCESS:
                System.out.println("Create zephyr testresult pass");
                reportTestSuccess(testCaseKey);
                break;

            case ITestResult.FAILURE:
                if (result.getAttribute(TestcaseType.TABLE_COMPARE) == null) {
                    System.out.println("Create zephyr testresult fail and jira defect");
                    processFailure(testCaseKey, table, req, testType);
                } else {
                    System.out.println("Create zephyr testresult fail by test1");
                    zephyr.reportResult(runKey, testCaseKey, "Fail", defectKeyTableDataCompare, req);
                }
                break;
        }
    }

    private void processFailure(String testCaseKey, AppDataCsv table, DefectRequest req, String testType) {
        String defectKey = jira.createDefect(req);
        List<String> rawFilePaths = getFilePaths(table, testType);
        jira.attachFiles(defectKey, rawFilePaths, "file");

        String testResultId = zephyr.reportResult(runKey, testCaseKey, "Fail", defectKey, req);

        if (testResultId != null && !testResultId.isEmpty()) {
            List<String> reportFiles = buildReportFileList(table.getTableName());
            zephyr.attachFiles(runKey, testCaseKey, testResultId, reportFiles, "file");
        } else {
            LogUtil.error("Cannot attach files to Zephyr: testResultId is null");
        }
    }

    private List<String> buildReportFileList(String tableName) {
        String upperTable = tableName.toUpperCase();
        return Arrays.asList(
                FileConfig.COMPARE_REPORT.replace("{table}", upperTable),
                FileConfig.TABLE_DATA_REPORT.replace("{table}", upperTable).replace("{role}", sourceDb.getDbRole().toString().toUpperCase()),
                FileConfig.TABLE_DATA_REPORT.replace("{table}", upperTable).replace("{role}", targetDb.getDbRole().toString().toUpperCase())
        );
    }

    private void validateZephyrConnection(String key) {
        try {
            isCycleExist = zephyr.isTestRunExist(key);

            if (!isCycleExist) {
                LogUtil.error("Zephyr Test Cycle " + key + " does not exist. Cannot proceed.");
                throw new RuntimeException();
            }
        } catch (Exception e) {
            LogUtil.error("Zephyr Test Cycle " + key + " does not exist. Cannot proceed.");
            throw new RuntimeException(e);
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
                break;
        }

        return filePaths;
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
