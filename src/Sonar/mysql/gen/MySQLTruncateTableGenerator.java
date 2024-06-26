package Sonar.mysql.gen;

import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.mysql.MySQLGlobalState;

public final class MySQLTruncateTableGenerator {

    private MySQLTruncateTableGenerator() {
    }

    public static SQLQueryAdapter generate(MySQLGlobalState globalState) {
        StringBuilder sb = new StringBuilder("TRUNCATE TABLE ");
        sb.append(globalState.getSchema().getRandomTable().getName());
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors.from("doesn't have this option",
                "Compression failed with the following error : Punch hole not supported by the filesystem or the tablespace page size is not large enough."));
    }

}
