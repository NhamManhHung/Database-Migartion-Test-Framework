package auto.framework.integration;

import auto.framework.models.dto.HttpResponse;
import auto.framework.reporting.DefectRequest;
import auto.framework.utils.ConfigUtil;
import auto.framework.utils.HttpClientUtil;
import auto.framework.utils.JsonUtil;
import auto.framework.utils.LogUtil;

public class JiraService implements IJiraService {

    private final String token =
            ConfigUtil.getEnv("jira.auth.token");

    @Override
    public String createDefect(DefectRequest req) {

        try {
            String url = ConfigUtil.getEnv("jira.post.issue.url");

            String template =
                    JsonUtil.read(
                            "file/jira-create-defect.json"
                    );

            String body = template
                    .replace("{{summary}}",
                            JsonUtil.escape(req.getSummary()))
                    .replace("{{description}}",
                            JsonUtil.escape(req.getDescription()))
                    .replace("{{epicLink}}",
                            JsonUtil.escape(req.getEpicLink()));

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
}
