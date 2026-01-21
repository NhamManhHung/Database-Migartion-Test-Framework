package auto.framework.integration;

import auto.framework.models.dto.HttpResponse;
import auto.framework.models.enums.FileConfig;
import auto.framework.reporting.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

import java.io.File;

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
    public void attachFile(String defectKey, File file) {
        try {
            String url = ConfigUtil.get(
                    "jira.post.attachment.url",
                    FileConfig.APPLICATION_FILE
            ).replace("{{issueKey}}", issueKey);

            HttpClientUtil.postMultipart(
                    url,
                    token,
                    file
            );

        } catch (Exception e) {
            LogUtil.error("Attach file failed: " + e.getMessage());
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
