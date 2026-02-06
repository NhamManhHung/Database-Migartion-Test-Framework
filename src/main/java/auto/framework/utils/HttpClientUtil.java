package auto.framework.utils;

import auto.framework.models.connection.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HttpClientUtil {

    public static HttpResponse post(
            String url,
            String token,
            String body
    ) throws Exception {

        HttpURLConnection conn =
                (HttpURLConnection) new URL(url).openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();
        String response = read(conn);

        return new HttpResponse(status, response);
    }

    public static HttpResponse get(
            String url,
            String token
    ) throws Exception {

        HttpURLConnection conn =
                (HttpURLConnection) new URL(url).openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");

        int status = conn.getResponseCode();
        String response = read(conn);

        return new HttpResponse(status, response);
    }

    private static String read(HttpURLConnection conn) throws Exception {
        InputStream is =
                conn.getResponseCode() >= 400
                        ? conn.getErrorStream()
                        : conn.getInputStream();

        if (is == null) return "";

        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static HttpResponse postMultipart(String url, String token, String fieldName, List<String> files) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("X-Atlassian-Token", "no-check");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            for (String fileString : files) {
                File file =  new File(fileString);
                builder.addBinaryBody(
                        fieldName,
                        file,
                        ContentType.DEFAULT_BINARY,
                        file.getName()
                );
            }

            post.setEntity(builder.build());

            try (CloseableHttpResponse response = client.execute(post)) {
                return HttpResponse.from(response);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
