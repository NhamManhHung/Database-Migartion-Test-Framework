package auto.framework.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonUtil {

    private JsonUtil() {
    }

    public static String read(String path) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)) {

            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Cannot read resource: " + path, e);
        }
    }

    public static String escape(String value) {
        if (value == null) return "";

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

