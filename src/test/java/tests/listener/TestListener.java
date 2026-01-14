package tests.listener;

import auto.framework.models.dto.CountResult;
import auto.framework.models.dto.DuplicateResult;
import auto.framework.models.dto.KeyMatchingResult;
import auto.framework.models.dto.TableInfoCSV;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TestListener implements ITestListener {
    public static final String ATTR_KEY_MATCHING_RESULT = "KEY_MATCHING_RESULT";
    public static final String ATTR_COUNT_RESULT = "COUNT_RESULT";
    public static final String ATTR_DUPLICATE_RESULT = "DUPLICATE_RESULT";

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

        Optional.ofNullable((KeyMatchingResult) result.getAttribute(ATTR_KEY_MATCHING_RESULT))
                .ifPresent(this::logResultKeyMatching);

        Optional.ofNullable((CountResult) result.getAttribute(ATTR_COUNT_RESULT))
                .ifPresent(this::logResultCount);

        Optional.ofNullable((DuplicateResult) result.getAttribute(ATTR_DUPLICATE_RESULT))
                .ifPresent(this::logResultDuplicates);
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
            case "HASH" -> table.getTcKeyMatching();
            case "DUPLICATE" -> table.getTcKeyDuplicate();
            case "COUNT" -> table.getTcKeyCount();
            default -> null;
        };

        return String.format("%s | %s | %s", tcKey, table.getTableName(), testType);
    }

    public void logResultKeyMatching(KeyMatchingResult r) {
        System.out.println(
                "\n[KEY-MATCHING] Table: " + r.getTableName() +
                        "\n       Oracle rows     : " + r.getTotalOracle() +
                        "\n       Postgres rows   : " + r.getTotalPostgres() +
                        "\n       Mismatch        : " + r.getMismatch() +
                        "\n       Missing Oracle  : " + r.getMissingInOracle() +
                        "\n       Missing Postgres: " + r.getMissingInPostgres()
        );
    }

    public void logResultDuplicates(DuplicateResult r) {
        System.out.println(
                "\n[DUPLICATE] Table: " + r.getTableName() +
                        "\n       Oracle duplicate count  : " + r.getDuplicateInOracle() +
                        "\n       Postgres duplicate count: " + r.getDuplicateInPostgres()
        );
    }

    public void logResultCount(CountResult r) {
        System.out.println(
                "\n[COUNT] Table: " + r.getTableName() +
                        "\n       Oracle rows  : " + r.getTotalOracle() +
                        "\n       Postgres rows: " + r.getTotalPostgres()
        );
    }
}