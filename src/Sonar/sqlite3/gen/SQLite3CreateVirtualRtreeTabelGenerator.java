package Sonar.sqlite3.gen;

import java.util.ArrayList;
import java.util.List;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.DBMSCommon;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Column;

public final class SQLite3CreateVirtualRtreeTabelGenerator {

    private SQLite3CreateVirtualRtreeTabelGenerator() {
    }

    public static SQLQueryAdapter createRandomTableStatement(SQLite3GlobalState globalState) {
        if (globalState.getSchema().getTables().getTables()
                .size() > globalState.getDbmsSpecificOptions().maxNumTables) {
            throw new IgnoreMeException();
        }
        return createTableStatement(globalState.getSchema().getFreeRtreeTableName(), globalState);
    }

    public static SQLQueryAdapter createTableStatement(String rTreeTableName, SQLite3GlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        List<SQLite3Column> columns = new ArrayList<>();
        StringBuilder sb = new StringBuilder("CREATE VIRTUAL TABLE ");
        sb.append(rTreeTableName);
        sb.append(" USING ");
        sb.append(Randomly.fromOptions("rtree_i32", "rtree"));
        sb.append("(");
        int size = 3 + Randomly.smallNumber();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            SQLite3Column c = SQLite3Common.createColumn(globalState.getColumns());
            globalState.addColumns(1);
            columns.add(c);
            sb.append(c.getName());
        }
        for (int i = 0; i < Randomly.smallNumber(); i++) {
            sb.append(", ");
            sb.append("+");
            String columnName = DBMSCommon.createColumnName(globalState.getColumns());
            globalState.addColumns(1);
            SQLite3ColumnBuilder columnBuilder = new SQLite3ColumnBuilder().allowPrimaryKey(false).allowNotNull(false)
                    .allowUnique(false).allowCheck(false);
            String c = columnBuilder.createColumn(columnName, globalState, columns);
            sb.append(c);
            sb.append(" ");
        }
        errors.add("virtual tables cannot use computed columns");
        sb.append(")");

        errors.add("Wrong number of columns for an rtree table");
        errors.add("Too many columns for an rtree table");
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

}
