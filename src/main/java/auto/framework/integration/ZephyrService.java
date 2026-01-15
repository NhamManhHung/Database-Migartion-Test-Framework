package auto.framework.integration;

import auto.framework.models.dto.HttpResponse;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

public class ZephyrService implements IZephyrService {

    private final String token =
            ConfigUtil.getEnv("jira.auth.token");

    @Override
    public boolean isTestRunExist(String runKey) {

        try {
            String url = ConfigUtil
                    .getEnv("zephyr.get.testrun.url")
                    .replace("{runKey}", runKey);

            HttpResponse res =
                    HttpClientUtil.get(url, token);

            return res.getStatus() == 200;

        } catch (Exception e) {
            LogUtil.error("Zephyr check failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void reportResult(
            String runKey,
            String testCaseKey,
            String status,
            String defectKey
    ) {
        try {
            String url = ConfigUtil
                    .getEnv("zephyr.post.result.url")
                    .replace("{runKey}", runKey)
                    .replace("{testCaseKey}", testCaseKey);

            String template = defectKey == null
                    ? JsonUtil.read(
                    "file/zephyr-testresult-pass.json"
            )
                    : JsonUtil.read(
                    "file/zephyr-testresult-fail.json"
            );

            String body = template
                    .replace("{{status}}",
                            JsonUtil.escape(status))
                    .replace("{{defectKey}}",
                            defectKey == null
                                    ? ""
                                    : JsonUtil.escape(defectKey));

            System.out.println("Zephyr TestResult Body:\n" + body);
            HttpResponse res =
                    HttpClientUtil.post(url, token, body.toString());
            System.out.println("Status Code:" + res.getStatus());
        } catch (Exception e) {
            LogUtil.error("Zephyr report exception: " + e.getMessage());
        }
    }
}
