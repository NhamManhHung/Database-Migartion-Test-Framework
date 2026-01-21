package tests.cds;

import auto.framework.helpers.HashCompareHelper;
import auto.framework.models.dto.*;
import auto.framework.models.enums.TestcaseType;
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
            description = TestcaseType.KEY_MATCHING
    )
    public void testTableMigration(TableInfoCSV table) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        String tableName = table.getTableName();
        String pk = table.getPrimaryKeyColumn();
        Future<Map<String, String>> oracleFuture =
                executor.submit(() -> DbUtil.queryToHashMap(oracle, tableName, pk));

        Future<Map<String, String>> postgresFuture =
                executor.submit(() -> DbUtil.queryToHashMap(postgres, tableName, pk));

        KeyMatchingResult keyMatchingResult = HashCompareHelper.compareHash(oracleFuture.get(), postgresFuture.get(), tableName);

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, keyMatchingResult);

        Assert.assertEquals(keyMatchingResult.getMismatch(), 0, "DATA MISMATCH");
    }

    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.COUNT
    )
    public void testTableCountMigration(TableInfoCSV table) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        String tableName = table.getTableName();

        Future<Integer> oracleFuture =
                executor.submit(() -> DbUtil.queryCount(oracle, tableName));

        Future<Integer> postgresFuture =
                executor.submit(() -> DbUtil.queryCount(postgres, tableName));

        int oracleCount = oracleFuture.get();
        int postgresCount = postgresFuture.get();

        CountResult countResult = HashCompareHelper.compareCount(oracleFuture.get(), postgresFuture.get(), tableName);

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, countResult);

        Assert.assertEquals(
                postgresCount,
                oracleCount,
                "Record count mismatch for table " + tableName +
                        " (oracle=" + oracleCount +
                        ", postgres=" + postgresCount + ")"
        );
    }

    @Test(
            dataProvider = "tableMigrationProvider",
            dataProviderClass = MigrationDataProvider.class,
            description = TestcaseType.DUPLICATE
    )
    public void testTableDuplicateId(TableInfoCSV table) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        String tableName = table.getTableName();
        String pkColumns = table.getPrimaryKeyColumn();

        Future<List<String>> oracleFuture =
                executor.submit(() ->
                        DbUtil.queryDuplicateKeys(oracle, tableName, pkColumns));

        Future<List<String>> postgresFuture =
                executor.submit(() ->
                        DbUtil.queryDuplicateKeys(postgres, tableName, pkColumns));

        List<String> oracleDuplicates = oracleFuture.get();
        List<String> postgresDuplicates = postgresFuture.get();
        DuplicateResult duplicateResult = new DuplicateResult(
                tableName,
                oracleDuplicates.size(),
                postgresDuplicates.size()
        );

        Reporter.getCurrentTestResult()
                .setAttribute(TestcaseType.DETAIL_DATA, duplicateResult);

        Assert.assertTrue(
                oracleDuplicates.isEmpty(),
                "Oracle has duplicate PKs in table " + tableName + ": " + oracleDuplicates
        );

        Assert.assertTrue(
                postgresDuplicates.isEmpty(),
                "Postgres has duplicate PKs in table " + tableName + ": " + postgresDuplicates
        );
    }
}
