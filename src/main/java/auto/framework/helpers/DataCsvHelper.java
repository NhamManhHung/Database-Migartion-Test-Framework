package auto.framework.helpers;

import auto.framework.models.csv.AppDataCsv;
import auto.framework.models.enums.FileConfig;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;

import java.io.InputStream;
import java.util.List;

public class DataCsvHelper {
    private static List<AppDataCsv> tables;

    public static synchronized List<AppDataCsv> getTables() {
        if (tables == null) {
            try {
                CsvUtil<AppDataCsv> csv = new CsvUtil<>(AppDataCsv.class);
                try (InputStream is = DataCsvHelper.class.getClassLoader()
                        .getResourceAsStream(
                                FileConfig.ALL_TABLE_DATA_PATH.replace("{app}", ConfigUtil.getEnv("app.name"))
                        )) {
                    tables = csv.read(is);
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot load tables from CSV", e);
            }
        }
        return tables;
    }
}
