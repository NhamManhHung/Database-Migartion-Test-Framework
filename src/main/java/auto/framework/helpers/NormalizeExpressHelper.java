package auto.framework.helpers;

import java.sql.ResultSetMetaData;
import java.util.*;

public class NormalizeExpressHelper {

    public static List<String> buildNormalizeExpressions(
            ResultSetMetaData meta, String dbType, List<String> pkColumns
    ) throws Exception {

        List<String> exprList = new ArrayList<>();

        for (int i = 1; i <= meta.getColumnCount(); i++) {

            String col = meta.getColumnName(i);
            if (pkColumns.stream().anyMatch(pk -> pk.equalsIgnoreCase(col))) {
                continue;
            }

            String typeName = meta.getColumnTypeName(i);

            String rule = RuleConfigHelper.getRule(dbType, typeName);

            //System.out.println(col + "------" + typeName + "--------" + rule);

            String expr = rule.replace("{col}", col);

            exprList.add(expr);
        }

        return exprList;
    }
}

