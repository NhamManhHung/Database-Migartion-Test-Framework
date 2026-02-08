package tests.cds;

import auto.framework.helpers.HashCompareHelper;
import auto.framework.helpers.PrimaryKeyExportHelper;
import auto.framework.models.csv.*;
import auto.framework.models.enums.FileConfig;
import auto.framework.models.enums.TestcaseType;
import auto.framework.models.result.CountResult;
import auto.framework.models.result.DuplicateResult;
import auto.framework.models.result.KeyMatchingResult;
import auto.framework.models.result.TableCompareResult;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import auto.framework.utils.DbUtil;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.common.BaseTest;
import tests.testdata.MigrationDataProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MigrationCDS extends BaseTest {
    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.COUNT
    )
    public void testTableCountMigration(AppDataCsv table) throws Exception {
        System.out.println("fuhjwerufgjerw");
        if (shouldSkipTable(table)) {
            System.out.println("fail testcase");
            Reporter.getCurrentTestResult()
                    .setAttribute(TestcaseType.TABLE_COMPARE, "Fail by first testcase");
            Assert.fail();
        }
        System.out.println("vgregf");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        String tableName = table.getTableName();

        Future<Integer> sourceFuture =
                executor.submit(() -> DbUtil.queryCount(sourceDb, tableName));

        Future<Integer> targetFuture =
                executor.submit(() -> DbUtil.queryCount(targetDb, tableName));

        int sourceCount = sourceFuture.get();
        int targetCount = targetFuture.get();

        CountResult countResult = HashCompareHelper.compareCount(sourceCount, targetCount, tableName);

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, countResult);

        Assert.assertEquals(
                sourceCount,
                targetCount,
                "Record count mismatch for table " + tableName +
                        " (source=" + sourceCount +
                        ", target=" + targetCount + ")"
        );
    }

    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.DUPLICATE
    )
    public void testTableDuplicateId(AppDataCsv table) throws Exception {
        if (shouldSkipTable(table)) {
            Reporter.getCurrentTestResult()
                    .setAttribute(TestcaseType.TABLE_COMPARE, "Fail by first testcase");
            Assert.fail();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);

        String tableName = table.getTableName();
        String pkColumns = table.getPrimaryKeyColumn();

        Future<List<String>> sourceFuture =
                executor.submit(() ->
                        DbUtil.queryDuplicateKeys(sourceDb, tableName, pkColumns));

        Future<List<String>> targetFuture =
                executor.submit(() ->
                        DbUtil.queryDuplicateKeys(targetDb, tableName, pkColumns));

        List<String> sourceDuplicates = sourceFuture.get();
        List<String> targetDuplicates = targetFuture.get();
        DuplicateResult duplicateResult = new DuplicateResult(
                tableName,
                sourceDuplicates.size(),
                targetDuplicates.size()
        );

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, duplicateResult);

        Assert.assertTrue(
                sourceDuplicates.isEmpty(),
                "Source has duplicate PKs in table " + tableName + ": " + sourceDuplicates
        );

        Assert.assertTrue(
                targetDuplicates.isEmpty(),
                "Target has duplicate PKs in table " + tableName + ": " + targetDuplicates
        );
    }

    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.KEY_MATCHING
    )
    public void testTableMigration(AppDataCsv table) throws Exception {
        if (shouldSkipTable(table)) {
            Reporter.getCurrentTestResult()
                    .setAttribute(TestcaseType.TABLE_COMPARE, "Fail by first testcase");
            Assert.fail();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        String tableName = table.getTableName();
        String pk = table.getPrimaryKeyColumn();
        Future<Map<String, String>> sourceFuture =
                executor.submit(() -> DbUtil.queryToHashMap(sourceDb, tableName, pk));

        Future<Map<String, String>> targetFuture =
                executor.submit(() -> DbUtil.queryToHashMap(targetDb, tableName, pk));

        KeyMatchingResult keyMatchingResult = HashCompareHelper.compareHash(sourceFuture.get(), targetFuture.get(), tableName);

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, keyMatchingResult);

        Assert.assertEquals(keyMatchingResult.getMismatch(), 0, "DATA MISMATCH");
    }

    private boolean shouldSkipTable(AppDataCsv table) {
        System.out.println(sourceDataSet);
        boolean a = sourceDataSet.stream()
                .noneMatch(s -> s.trim().equalsIgnoreCase(table.getTableName().trim()));
        System.out.println("Skip: " + a);
        return a;
    }

}
