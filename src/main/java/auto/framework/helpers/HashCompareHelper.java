package auto.framework.helpers;

import auto.framework.models.dto.CountResult;
import auto.framework.models.dto.KeyMatchingResult;
import auto.framework.models.dto.ReportCSV;
import auto.framework.models.enums.FileConfig;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HashCompareHelper {
    /**
     * COMPARE DATA
     */

    public static KeyMatchingResult compareHash(
            Map<String, String> oracle,
            Map<String, String> postgres,
            String table
    ) throws Exception {

        List<ReportCSV> report = new ArrayList<>();

        int missPg = 0, missOra = 0, mismatch = 0;

        for (String id : oracle.keySet()) {
            if (!postgres.containsKey(id)) {
                missPg++;
                report.add(new ReportCSV(id, "MISSING IN POSTGRES"));
            } else if (!oracle.get(id).equals(postgres.get(id))) {
                mismatch++;
                report.add(new ReportCSV(id, "MISMATCH DATA"));
            } else {
                report.add(new ReportCSV(id, "PASSED"));
            }
        }

        for (String id : postgres.keySet()) {
            if (!oracle.containsKey(id)) {
                missOra++;
                report.add(new ReportCSV(id, "MISSING IN ORACLE"));
            }
        }
        String dataFile = FileConfig.REPORT_PATH.replace("{path}", table + ".csv" );
        new CsvUtil<>(ReportCSV.class)
                .write(dataFile, report);

        return new KeyMatchingResult(
                table,
                oracle.size(),
                postgres.size(),
                missOra,
                missPg,
                mismatch
        );
    }

    public static CountResult compareCount(
            int oracleCount,
            int postgresCount,
            String table
    ) throws Exception {

        int missOra = Math.max(0, postgresCount - oracleCount);
        int missPg = Math.max(0, oracleCount - postgresCount);

        return new CountResult(
                table,
                oracleCount,
                postgresCount,
                missOra,
                missPg
        );
    }
}
