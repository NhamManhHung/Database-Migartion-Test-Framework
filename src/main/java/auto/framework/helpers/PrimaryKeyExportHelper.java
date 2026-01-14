package auto.framework.helpers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrimaryKeyExportHelper {
    private static final Map<String, List<String>> PK_EXPORT =
            new ConcurrentHashMap<>();

    public static void registerIfAuto(
            String table,
            List<String> pkColumns
    ) {
        PK_EXPORT.putIfAbsent(table, pkColumns);
    }

    public static Map<String, List<String>> getAll() {
        return PK_EXPORT;
    }
}
