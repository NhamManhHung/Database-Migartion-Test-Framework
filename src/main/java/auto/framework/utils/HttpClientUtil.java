package auto.framework.utils;

import auto.framework.models.dto.HttpResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

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
}
