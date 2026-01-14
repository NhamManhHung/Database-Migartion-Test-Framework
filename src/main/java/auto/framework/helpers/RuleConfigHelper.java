package auto.framework.helpers;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class RuleConfigHelper {

    private static Map<String, Object> rules;

    public static void loadRules() {
        String ruleFile = "rule/convert_rule.yml";
        try (InputStream is = RuleConfigHelper.class
                .getClassLoader()
                .getResourceAsStream(ruleFile)) {

            if (is == null) throw new RuntimeException("Cannot find rule file: " + ruleFile);

            Yaml yaml = new Yaml();
            rules = yaml.load(is);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load rule file: " + ruleFile, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static String getRule(String dbType, String typeName) {

        Map<String, String> dbRules = (Map<String, String>) rules.get(dbType.toLowerCase());
        if (dbRules == null)
            throw new RuntimeException("No rules defined for DB type: " + dbType);

        typeName = typeName.toUpperCase();

        if (dbRules.containsKey(typeName)) {
            return dbRules.get(typeName);
        }

        for (String k : dbRules.keySet()) {
            if (typeName.contains(k)) {
                return dbRules.get(k);
            }
        }

        return dbRules.get("default");
    }
}
