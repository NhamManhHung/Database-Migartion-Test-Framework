package auto.framework.integration;

import auto.framework.models.connection.HttpResponse;
import auto.framework.models.enums.FileConfig;
import auto.framework.models.connection.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

import java.io.File;
import java.util.List;

public class JiraService implements IJiraService {

    private final String token =
            ConfigUtil.get("jira.auth.token", FileConfig.APPLICATION_FILE);

    @Override
    public String createDefect(DefectRequest req) {

        try {
            String url = ConfigUtil.get("jira.post.issue.url", FileConfig.APPLICATION_FILE);

            String template =
                    JsonUtil.read(
                            FileConfig.JIRA_DEFECT_TEMPLATE
                    );

            String labelsJson = buildLabelsJson(ConfigUtil.getEnv("jira.labels"));
            String body = template
                    .replace("{{summary}}",
                            JsonUtil.escape(req.getSummary()))
                    .replace("{{description}}",
                            JsonUtil.escape(req.getDescription()))
                    .replace("{{epicLink}}",
                            JsonUtil.escape(req.getEpicLink()))
                    .replace("\"{{labels}}\"", labelsJson);

            System.out.println("Jira Issue Body:\n" + body);

            HttpResponse res =
                    HttpClientUtil.post(url, token, body);

            String bodyRes = res.getBody();
            int idx = bodyRes.indexOf("\"key\"");
            if (idx > 0) {
                int start = bodyRes.indexOf("\"", idx + 6) + 1;
                int end = bodyRes.indexOf("\"", start);
                return bodyRes.substring(start, end);
            }

            throw new RuntimeException(
                    "Cannot parse issue key from response"
            );

        } catch (Exception e) {
            LogUtil.error("Jira create defect failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void attachFiles(String defectKey, List<String> filePaths, String fieldName) {

        if (filePaths == null || filePaths.isEmpty()) {
            LogUtil.warn("File list is empty. Skip attachment for issue " + defectKey);
            return;
        }

        String url = ConfigUtil.get(
                "jira.post.issue.file.url",
                FileConfig.APPLICATION_FILE
        ).replace("{issueKey}", defectKey);

        try {
            HttpResponse res = HttpClientUtil.postMultipart(
                    url,
                    token,
                    fieldName,
                    filePaths
            );

            if (res.getStatus() == 200 || res.getStatus() == 201) {
                LogUtil.info(
                        "Upload file success -> issue " + defectKey
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


    private static String buildLabelsJson(String labelsProp) {
        if (labelsProp == null || labelsProp.trim().isEmpty()) {
            return "";
        }

        String[] items = labelsProp.split(",");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(",");

            sb.append("\"")
                    .append(JsonUtil.escape(items[i].trim()))
                    .append("\"");
        }

        return sb.toString();
    }

}
