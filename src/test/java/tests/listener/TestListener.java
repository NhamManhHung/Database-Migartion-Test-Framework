package tests.listener;

import auto.framework.models.dto.*;
import auto.framework.models.enums.TestcaseType;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestListener implements ITestListener {

    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    @Override
    public void onTestStart(ITestResult result) {
        startTimes.put(getKey(result), System.nanoTime());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log(result, "PASSED");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log(result, "FAILED");

        Object detail = result.getAttribute(TestcaseType.DETAIL_DATA);

        if (detail instanceof KeyMatchingResult) {
            logResultKeyMatching((KeyMatchingResult) detail);

        } else if (detail instanceof CountResult) {
            logResultCount((CountResult) detail);

        } else if (detail instanceof DuplicateResult) {
            logResultDuplicates((DuplicateResult) detail);
        }

    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log(result, "SKIPPED");
    }

    private void log(ITestResult result, String status) {
        String key = getKey(result);
        long end = System.nanoTime();
        long start = startTimes.getOrDefault(key, end);
        long durationMs = (end - start) / 1_000_000;

        String displayName = resolveDisplayName(result);

        System.out.printf(
                "[%s] %s | %d ms%n",
                status,
                displayName,
                durationMs
        );
    }

    private String getKey(ITestResult result) {
        return result.getTestClass().getName()
                + "#"
                + result.getMethod().getMethodName()
                + result.getParameters().hashCode();
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println(
                "\n[SUITE] " + context.getName()
                        + " | Total tests: "
                        + context.getAllTestMethods().length
        );
    }

    private String resolveDisplayName(ITestResult result) {

        Object[] params = result.getParameters();
        String testType = result.getMethod().getDescription();
        if (params == null || params.length == 0) {
            return null;
        }

        Object param = params[0];
        if (!(param instanceof TableInfoCSV table)) {
            return null;
        }

        String tcKey = switch (testType) {
            case TestcaseType.KEY_MATCHING -> table.getTcKeyMatching();
            case TestcaseType.DUPLICATE -> table.getTcKeyDuplicate();
            case TestcaseType.COUNT -> table.getTcKeyCount();
            default -> null;
        };

        return String.format("%s | %s | %s", tcKey, table.getTableName(), testType);
    }

    public void logResultKeyMatching(KeyMatchingResult r) {
        System.out.println(
                "\n[ "+ TestcaseType.KEY_MATCHING + " ] Table: " + r.getTableName() +
                        "\n       Oracle rows     : " + r.getTotalOracle() +
                        "\n       Postgres rows   : " + r.getTotalPostgres() +
                        "\n       Mismatch        : " + r.getMismatch() +
                        "\n       Missing Oracle  : " + r.getMissingInOracle() +
                        "\n       Missing Postgres: " + r.getMissingInPostgres()
        );
    }

    public void logResultDuplicates(DuplicateResult r) {
        System.out.println(
                "\n[ "+ TestcaseType.DUPLICATE + " ] Table: " + r.getTableName() +
                        "\n       Oracle duplicate count  : " + r.getDuplicateInOracle() +
                        "\n       Postgres duplicate count: " + r.getDuplicateInPostgres()
        );
    }

    public void logResultCount(CountResult r) {
        System.out.println(
                "\n[ "+ TestcaseType.COUNT + " ] Table: " + r.getTableName() +
                        "\n       Oracle rows  : " + r.getTotalOracle() +
                        "\n       Postgres rows: " + r.getTotalPostgres()
        );
    }
}