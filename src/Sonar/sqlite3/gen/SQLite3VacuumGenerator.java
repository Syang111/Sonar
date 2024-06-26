package Sonar.sqlite3.gen;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3GlobalState;


public final class SQLite3VacuumGenerator {

    private SQLite3VacuumGenerator() {
    }

    public static SQLQueryAdapter executeVacuum(SQLite3GlobalState globalState) {
        StringBuilder sb = new StringBuilder("VACUUM");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("temp", "main"));
        }
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors.from("cannot VACUUM from within a transaction",
                "cannot VACUUM - SQL statements in progress", "The database file is locked"));
    }

}
