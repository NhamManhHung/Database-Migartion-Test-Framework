package auto.framework.helpers;

import auto.framework.models.connection.DefectRequest;
import auto.framework.models.result.CountResult;
import auto.framework.models.enums.TestcaseType;
import auto.framework.utils.ConfigUtil;

public class DefectBuilderHelper {

    public static DefectRequest build(
            String type,
            String table,
            Object detailData,
            String epicKey) {
        String appName = ConfigUtil.getEnv("app.name");
        String summary;
        String prefix = appName + ": " + table;

        String description = switch (type) {

            case TestcaseType.COUNT -> {
                CountResult data = (CountResult) detailData;

                summary = prefix + " record count is mismatch";

                yield prefix +
                        "\nSource = " + data.getTotalSource() +
                        "\nTarget = " + data.getTotalTarget();
            }

            case TestcaseType.DUPLICATE -> {
                summary = prefix + " has duplicate records";
                yield summary;
            }

            case TestcaseType.KEY_MATCHING -> {
                summary = prefix + " has mismatch data";
                yield summary;
            }

            default -> throw new RuntimeException("Unknown TestcaseType " + type);
        };


        return new DefectRequest(summary, description, epicKey);
    }
}
