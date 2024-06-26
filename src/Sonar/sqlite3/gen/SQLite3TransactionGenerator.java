package Sonar.sqlite3.gen;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3GlobalState;

public final class SQLite3TransactionGenerator {

    private SQLite3TransactionGenerator() {
    }

    public static SQLQueryAdapter generateCommit(SQLite3GlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append(Randomly.fromOptions("COMMIT", "END"));
        if (Randomly.getBoolean()) {
            sb.append(" TRANSACTION");
        }
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors.from("no transaction is active",
                "The database file is locked", "FOREIGN KEY constraint failed"), true);
    }

    public static SQLQueryAdapter generateBeginTransaction(SQLite3GlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN ");
        if (Randomly.getBoolean()) {
            sb.append(Randomly.fromOptions("DEFERRED", "IMMEDIATE", "EXCLUSIVE"));
        }
        sb.append(" TRANSACTION;");
        return new SQLQueryAdapter(sb.toString(),
                ExpectedErrors.from("cannot start a transaction within a transaction", "The database file is locked"));
    }

    public static SQLQueryAdapter generateRollbackTransaction(SQLite3GlobalState globalState) {

        return new SQLQueryAdapter("ROLLBACK TRANSACTION;",
                ExpectedErrors.from("no transaction is active", "The database file is locked"), true);
    }

}
