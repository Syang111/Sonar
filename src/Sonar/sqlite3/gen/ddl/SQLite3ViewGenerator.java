package Sonar.sqlite3.gen.ddl;

import java.sql.SQLException;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.DBMSCommon;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3Errors;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.SQLite3Options.SQLite3OracleFactory;
import Sonar.sqlite3.SQLite3Visitor;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.gen.SQLite3Common;
import Sonar.sqlite3.oracle.SQLite3RandomQuerySynthesizer;
import Sonar.sqlite3.schema.SQLite3Schema;

public final class SQLite3ViewGenerator {

    private SQLite3ViewGenerator() {
    }

    public static SQLQueryAdapter dropView(SQLite3GlobalState globalState) {
        SQLite3Schema s = globalState.getSchema();
        StringBuilder sb = new StringBuilder("DROP VIEW ");
        sb.append(s.getRandomViewOrBailout().getName());
        return new SQLQueryAdapter(sb.toString(), true);
    }

    public static SQLQueryAdapter generate(SQLite3GlobalState globalState) throws SQLException {
        if (globalState.getSchema().getTables().getTables()
                .size() >= globalState.getDbmsSpecificOptions().maxNumTables) {
            throw new IgnoreMeException();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE");
        if (globalState.getDbmsSpecificOptions().testTempTables && Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("TEMP", "TEMPORARY"));
        }
        sb.append(" VIEW ");
        if (Randomly.getBoolean()) {
            sb.append(" IF NOT EXISTS ");
        }
        sb.append(SQLite3Common.getFreeViewName(globalState.getSchema()));
        ExpectedErrors errors = new ExpectedErrors();
        SQLite3Errors.addExpectedExpressionErrors(errors);
        errors.add("is circularly defined");
        errors.add("unsupported frame specification");
        errors.add("The database file is locked");
        int size = 1 + Randomly.smallNumber();
        columnNamesAs(sb, size, globalState);
        SQLite3Expression randomQuery;
        do {
            randomQuery = SQLite3RandomQuerySynthesizer.generate(globalState, size);
        } while (globalState.getDbmsSpecificOptions().oracles == SQLite3OracleFactory.PQS
                && !checkAffinity(randomQuery));
        sb.append(SQLite3Visitor.asString(randomQuery));
        return new SQLQueryAdapter(sb.toString(), errors, true);

    }

    
    private static boolean checkAffinity(SQLite3Expression randomQuery) {
        if (randomQuery instanceof SQLite3Select) {
            for (SQLite3Expression expr : ((SQLite3Select) randomQuery).getFetchColumns()) {
                if (expr.getExpectedValue() == null || expr.getAffinity() != null
                        || expr.getImplicitCollateSequence() != null || expr.getExplicitCollateSequence() != null) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static void columnNamesAs(StringBuilder sb, int size, SQLite3GlobalState globalState) {
        sb.append("(");
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(DBMSCommon.createColumnName(globalState.getColumns()));
            globalState.addColumns(1);
        }
        sb.append(")");
        sb.append(" AS ");
    }

}
