package auto.framework.helpers;

import auto.framework.models.result.CountResult;
import auto.framework.models.result.KeyMatchingResult;
import auto.framework.models.csv.CompareReportCsv;
import auto.framework.models.enums.FileConfig;
import auto.framework.utils.CsvUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HashCompareHelper {
    /**
     * COMPARE DATA
     */

    public static KeyMatchingResult compareHash(
            Map<String, String> source,
            Map<String, String> target,
            String table
    ) throws Exception {

        List<CompareReportCsv> report = new ArrayList<>();

        int missTarget = 0, missSource = 0, mismatch = 0;

        for (String id : source.keySet()) {
            if (!target.containsKey(id)) {
                missTarget++;
                report.add(new CompareReportCsv(id, "MISSING IN TARGET"));
            } else if (!source.get(id).equals(target.get(id))) {
                mismatch++;
                report.add(new CompareReportCsv(id, "MISMATCH DATA"));
            } else {
                report.add(new CompareReportCsv(id, "PASSED"));
            }
        }

        for (String id : target.keySet()) {
            if (!source.containsKey(id)) {
                missSource++;
                report.add(new CompareReportCsv(id, "MISSING IN SOURCE"));
            }
        }
        String dataFile = FileConfig.COMPARE_REPORT
                .replace("{table}", table.toUpperCase());

        new CsvUtil<>(CompareReportCsv.class)
                .write(dataFile, report);

        return new KeyMatchingResult(
                table,
                source.size(),
                target.size(),
                missSource,
                missTarget,
                mismatch
        );
    }

    public static CountResult compareCount(
            int sourceCount,
            int targetCount,
            String table
    ) throws Exception {

        int missOra = Math.max(0, targetCount - sourceCount);
        int missPg = Math.max(0, sourceCount - targetCount);

        return new CountResult(
                table,
                sourceCount,
                targetCount,
                missOra,
                missPg
        );
    }
}
