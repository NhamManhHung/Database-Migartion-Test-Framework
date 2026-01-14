package tests.testdata;

import auto.framework.models.dto.TableInfoCSV;
import auto.framework.utils.CsvUtil;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class MigrationDataProvider {
    @DataProvider(name = "tableMigrationProvider")
    public static Object[][] tableMigrationProvider() throws IOException {
        CsvUtil<TableInfoCSV> csv = new CsvUtil<>(TableInfoCSV.class);
        List<TableInfoCSV> tables;
        try (InputStream is = MigrationDataProvider.class.getClassLoader()
                .getResourceAsStream("data/MBF_CDS.csv")) {
            tables = csv.read(is);
        }

        Object[][] data = new Object[tables.size()][1];
        for (int i = 0; i < tables.size(); i++) {
            data[i][0] = tables.get(i);
        }
        return data;
    }
}
