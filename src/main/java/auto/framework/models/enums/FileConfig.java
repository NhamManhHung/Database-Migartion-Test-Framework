package auto.framework.models.enums;

public class FileConfig {
    public static final String ALL_TABLE_DATA_PATH = "data/{app}.csv";
    public static final String ALL_TABLE_DATA_REPORT = "report/{app}";
    public static final String TABLE_DATA_REPORT = "report/{table}_{role}";
    public static final String COMPARE_REPORT = "report/{table}_COMPARE";
    public static final String DUPLICATE_REPORT = "report/{table}_DUPLICATE_{role}";
    public static final String ENV_DIR = "env/";
    public static final String DEFAULT_ENV = "env.properties";
    public static final String APPLICATION_FILE = "config/application.properties";
    public static final String RULE_FILE = "config/convert_rule.yml";
    public static final String JIRA_DEFECT_TEMPLATE = "file/jira-create-defect.json";
    public static final String ZEPHYR_RESULT_PASS = "file/zephyr-testresult-pass.json";
    public static final String ZEPHYR_RESULT_FAIL = "file/zephyr-testresult-fail.json";

}
