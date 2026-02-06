package auto.framework.models.connection;

import lombok.Getter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Getter
public class HttpResponse {
    private final int status;
    private final String body;

    public HttpResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
    public static HttpResponse from(CloseableHttpResponse response) {
        try {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            return new HttpResponse(statusCode, body);
        } catch (IOException e) {
            return new HttpResponse(500, "Error reading response");
        }
    }

}
