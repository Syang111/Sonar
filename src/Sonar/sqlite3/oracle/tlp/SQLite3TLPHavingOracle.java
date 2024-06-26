package Sonar.sqlite3.oracle.tlp;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.ComparatorHelper;
import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.ExpectedErrors;
import Sonar.sqlite3.SQLite3Errors;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.SQLite3Visitor;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Expression.Join;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixUnaryOperation;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixUnaryOperation.PostfixUnaryOperator;
import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.ast.SQLite3Select.SelectType;
import Sonar.sqlite3.ast.SQLite3UnaryOperation;
import Sonar.sqlite3.ast.SQLite3UnaryOperation.UnaryOperator;
import Sonar.sqlite3.gen.SQLite3Common;
import Sonar.sqlite3.gen.SQLite3ExpressionGenerator;
import Sonar.sqlite3.schema.SQLite3Schema;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Column;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Table;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Tables;

public class SQLite3TLPHavingOracle implements TestOracle<SQLite3GlobalState> {

    private final SQLite3GlobalState state;
    private final ExpectedErrors errors = new ExpectedErrors();
    private String generatedQueryString;

    public SQLite3TLPHavingOracle(SQLite3GlobalState state) {
        this.state = state;
        SQLite3Errors.addExpectedExpressionErrors(errors);
        errors.add("no such column");
        errors.add("ON clause references tables to its right");
    }

    @Override
    public void check() throws SQLException {
        SQLite3Schema s = state.getSchema();
        SQLite3Tables targetTables = s.getRandomTableNonEmptyTables();
        List<SQLite3Expression> groupByColumns = Randomly.nonEmptySubset(targetTables.getColumns()).stream()
                .map(c -> new SQLite3ColumnName(c, null)).collect(Collectors.toList());
        List<SQLite3Column> columns = targetTables.getColumns();
        SQLite3ExpressionGenerator gen = new SQLite3ExpressionGenerator(state).setColumns(columns);
        SQLite3Select select = new SQLite3Select();
        select.setFetchColumns(groupByColumns);
        List<SQLite3Table> tables = targetTables.getTables();
        List<Join> joinStatements = gen.getRandomJoinClauses(tables);
        List<SQLite3Expression> from = SQLite3Common.getTableRefs(tables, state.getSchema());
        select.setJoinClauses(joinStatements);
        select.setSelectType(SelectType.ALL);
        select.setFromTables(from);

        select.setGroupByClause(groupByColumns);
        select.setHavingClause(null);
        String originalQueryString = SQLite3Visitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        SQLite3Expression predicate = gen.getHavingClause();
        select.setHavingClause(predicate);
        String firstQueryString = SQLite3Visitor.asString(select);
        select.setHavingClause(new SQLite3UnaryOperation(UnaryOperator.NOT, predicate));
        String secondQueryString = SQLite3Visitor.asString(select);
        select.setHavingClause(new SQLite3PostfixUnaryOperation(PostfixUnaryOperator.ISNULL, predicate));
        String thirdQueryString = SQLite3Visitor.asString(select);
        String combinedString = firstQueryString + " UNION ALL " + secondQueryString + " UNION ALL " + thirdQueryString;
        if (combinedString.contains("EXIST")) {
            throw new IgnoreMeException();
        }
        List<String> secondResultSet = ComparatorHelper.getResultSetFirstColumnAsString(combinedString, errors, state);
        if (state.getOptions().logEachSelect()) {
            state.getLogger().writeCurrent(originalQueryString);
            state.getLogger().writeCurrent(combinedString);
        }
        if (new HashSet<>(resultSet).size() != new HashSet<>(secondResultSet).size()) {
            throw new AssertionError(originalQueryString + ";\n" + combinedString + ";");
        }
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}
