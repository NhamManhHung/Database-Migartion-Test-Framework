package auto.framework.integration;

public class IntegrationProvider {

    private static final IZephyrService ZEPHYR =
            new ZephyrService();

    private static final IJiraService JIRA =
            new JiraService();

    public static IZephyrService zephyr() {
        return ZEPHYR;
    }

    public static IJiraService jira() {
        return JIRA;
    }
}
