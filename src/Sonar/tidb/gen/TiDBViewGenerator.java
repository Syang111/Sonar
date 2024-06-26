package Sonar.tidb.gen;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.tidb.TiDBErrors;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;

public final class TiDBViewGenerator {

    private TiDBViewGenerator() {
    }

    public static SQLQueryAdapter getQuery(TiDBGlobalState globalState) {
        if (globalState.getSchema().getDatabaseTables().size() > globalState.getDbmsSpecificOptions().maxNumTables) {
            throw new IgnoreMeException();
        }
        int nrColumns = Randomly.smallNumber() + 1;
        StringBuilder sb = new StringBuilder("CREATE ");
        if (Randomly.getBoolean()) {
            sb.append("OR REPLACE ");
        }
        if (Randomly.getBoolean()) {
            sb.append("ALGORITHM=");
            sb.append(Randomly.fromOptions("UNDEFINED", "MERGE", "TEMPTABLE"));
            sb.append(" ");
        }
        sb.append("VIEW ");
        sb.append(globalState.getSchema().getFreeViewName());
        sb.append("(");
        for (int i = 0; i < nrColumns; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("c");
            sb.append(i);
        }
        sb.append(") AS ");
        sb.append(TiDBRandomQuerySynthesizer.generate(globalState, nrColumns).getQueryString());
        ExpectedErrors errors = new ExpectedErrors();
        TiDBErrors.addExpressionErrors(errors);
        errors.add(
                "references invalid table(s) or column(s) or function(s) or definer/invoker of view lack rights to use them");
        errors.add("Unknown column ");
        if (sb.toString().contains("\\\\")) {

            throw new IgnoreMeException();
        }
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

}
