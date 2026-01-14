package auto.framework.utils;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigUtil {

    private static final String ENV_DIR = "env/";
    private static final String DEFAULT_ENV = "env.properties";

    private static final Properties envProps = new Properties();

    private static final Map<String, Properties> propsCache = new ConcurrentHashMap<>();

    static {
        loadEnv();
    }

    private static void loadEnv() {
        try {
            String profile = System.getProperty("profiles-active");
            if (profile == null || profile.trim().isEmpty()) {
                profile = ENV_DIR + DEFAULT_ENV;
            }

            try (InputStream is = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(ENV_DIR + profile)) {

                if (is == null) {
                    throw new RuntimeException("Env config not found: " + profile);
                }

                envProps.load(is);
                //LogUtil.info("Loaded env config: " + profile);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load env config", e);
        }
    }

    public static String getEnv(String key) {
        String value = envProps.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing env key: " + key);
        }
        return value.trim();
    }

    public static boolean hasEnv(String key) {
        return envProps.containsKey(key);
    }

    public static String get(String fileName, String key) {
        Properties props = propsCache.computeIfAbsent(fileName, ConfigUtil::loadFile);
        String value = props.getProperty(key);

        if (value == null) {
            throw new RuntimeException(
                    "Missing key: " + key + " in file: " + fileName);
        }
        return value.trim();
    }

    private static Properties loadFile(String fileName) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(fileName)) {

            if (is == null) {
                throw new RuntimeException("Config file not found: " + fileName);
            }

            Properties props = new Properties();
            props.load(is);

            //LogUtil.info("Loaded config: " + fileName);
            return props;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + fileName, e);
        }
    }
}
