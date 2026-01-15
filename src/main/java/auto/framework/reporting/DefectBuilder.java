package auto.framework.reporting;

import auto.framework.models.dto.CountResult;
import auto.framework.models.enums.TestcaseType;

public class DefectBuilder {

    public static DefectRequest build(
            TestcaseType type,
            String table,
            CountResult detail,
            String epicKey) {

        String summary;
        String description = switch (type) {
            case COUNT -> {
                summary = table + " record count is mismatch";
                yield table +
                        "\\nSource = " + detail.getTotalOracle() +
                        "\\nTarget = " + detail.getTotalPostgres();
            }
            case DUPLICATE -> {
                summary = table + " has duplicate records";
                yield table + " has duplicate records";
            }
            default -> {
                summary = table + " has mismatch data";
                yield table + " has mismatch data";
            }
        };

        return new DefectRequest(summary, description, epicKey);
    }
}
