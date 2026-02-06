package auto.framework.integration;

import auto.framework.models.connection.HttpResponse;
import auto.framework.models.enums.FileConfig;
import auto.framework.models.connection.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

import java.util.List;

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
    public String reportResult(
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

            HttpResponse res = HttpClientUtil.post(url, token, body);
            if (res.getStatus() == 200 || res.getStatus() == 201) {
                String testResultId = JsonUtil.parseId(res.getBody(), "id");
                LogUtil.info("Test result created with ID: " + testResultId);
                return testResultId;
            } else {
                LogUtil.error("Failed to create test result. Status: " + res.getStatus());
                return null;
            }
        } catch (Exception e) {
            LogUtil.error("Zephyr report exception: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void attachFiles(
            String runKey,
            String testCaseKey,
            String testResultId,
            List<String> filePaths,
            String fieldName
    ) {

        if (filePaths == null || filePaths.isEmpty()) {
            LogUtil.warn("File list is empty. Skip attachment for issue " + testResultId);
            return;
        }

        String url = ConfigUtil
                .get("zephyr.post.result.file.url", FileConfig.APPLICATION_FILE)
                .replace("{runKey}", runKey)
                .replace("{testCaseKey}", testCaseKey)
                .replace("{testResultId}", testResultId);

        try {
            HttpResponse res = HttpClientUtil.postMultipart(
                    url,
                    token,
                    fieldName,
                    filePaths
            );

            if (res.getStatus() == 200 || res.getStatus() == 201) {
                LogUtil.info(
                        "Upload file success -> issue " + testResultId
                );
            } else {
                LogUtil.error(
                        "Upload file failed | Status=" + res.getStatus()
                );
                LogUtil.error("Response body: " + res.getBody());
            }

        } catch (Exception e) {
            LogUtil.error(
                    "Exception when uploading file | " + e.getMessage()
            );
        }

    }
}
