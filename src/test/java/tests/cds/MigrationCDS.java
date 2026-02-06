package tests.cds;

import auto.framework.helpers.HashCompareHelper;
import auto.framework.models.csv.*;
import auto.framework.models.enums.TestcaseType;
import auto.framework.models.result.CountResult;
import auto.framework.models.result.DuplicateResult;
import auto.framework.models.result.KeyMatchingResult;
import auto.framework.utils.DbUtil;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;
import tests.common.BaseTest;
import tests.testdata.MigrationDataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MigrationCDS extends BaseTest {


    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.COUNT
    )
    public void testTableCountMigration(AppDataCsv table) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        String tableName = table.getTableName();

        Future<Integer> sourceFuture =
                executor.submit(() -> DbUtil.queryCount(sourceDb, tableName));

        Future<Integer> targetFuture =
                executor.submit(() -> DbUtil.queryCount(targetDb, tableName));

        int sourceCount = sourceFuture.get();
        int targetCount = targetFuture.get();

        CountResult countResult = HashCompareHelper.compareCount(sourceFuture.get(), targetFuture.get(), tableName);

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
            dependsOnMethods = {"testTableCountMigration"},
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.DUPLICATE
    )
    public void testTableDuplicateId(AppDataCsv table) throws Exception {

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
            dependsOnMethods = {"testTableDuplicateId"},
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.KEY_MATCHING
    )
    public void testTableMigration(AppDataCsv table) throws Exception {
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

}
