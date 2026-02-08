package tests.testdata;

import auto.framework.helpers.DataCsvHelper;
import auto.framework.models.csv.AppDataCsv;
import auto.framework.models.enums.FileConfig;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.CsvUtil;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MigrationDataProvider {
    @DataProvider(name = "tableMigrationProvider")
    public static Object[][] tableMigrationProvider() throws IOException {
        List<AppDataCsv> tables = DataCsvHelper.getTables();

        Object[][] data = new Object[tables.size()][1];
        for (int i = 0; i < tables.size(); i++) {
            data[i][0] = tables.get(i);
        }
        return data;
    }
}
