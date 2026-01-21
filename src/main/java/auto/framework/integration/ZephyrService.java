package auto.framework.integration;

import auto.framework.models.dto.HttpResponse;
import auto.framework.models.enums.FileConfig;
import auto.framework.reporting.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

public class ZephyrService implements IZephyrService {

    private final String token =
            ConfigUtil.get("jira.auth.token", FileConfig.APPLICATION_FILE);

    @Override
    public boolean isTestRunExist(String runKey) {

        try {
            String url = ConfigUtil
                    .get("zephyr.get.testrun.url", FileConfig.APPLICATION_FILE)
                    .replace("{runKey}", runKey);

//            HttpResponse res =
//                    HttpClientUtil.get(url, token);

            //return res.getStatus() == 200;
            return true;
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
            String defectKey,
            DefectRequest req
    ) {
        try {
            String url = ConfigUtil
                    .get("zephyr.post.result.url", FileConfig.APPLICATION_FILE)
                    .replace("{runKey}", runKey)
                    .replace("{testCaseKey}", testCaseKey);

            String template = defectKey == null
                    ? JsonUtil.read(
                    FileConfig.ZEPHYR_RESULT_PASS
            )
                    : JsonUtil.read(
                    FileConfig.ZEPHYR_RESULT_FAIL
            );

            String body = template
                    .replace("{{env.name}}",
                            ConfigUtil.getEnv("env.name"))
                    .replace("{{defectKey}}",
                            defectKey == null
                                    ? ""
                                    : JsonUtil.escape(defectKey))
                    .replace("{{description}}", defectKey != null
                                    ? req.getDescription()
                                    : "");

            System.out.println("Zephyr TestResult Body:\n" + body);
            HttpResponse res =
                    HttpClientUtil.post(url, token, body);
        } catch (Exception e) {
            LogUtil.error("Zephyr report exception: " + e.getMessage());
        }
    }
}
