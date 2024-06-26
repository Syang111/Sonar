package Sonar.mysql.oracle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.SQLConnection;
import Sonar.common.oracle.PivotedQuerySynthesisBase;
import Sonar.common.query.Query;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.mysql.MySQLErrors;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLSchema.MySQLColumn;
import Sonar.mysql.MySQLSchema.MySQLRowValue;
import Sonar.mysql.MySQLSchema.MySQLTable;
import Sonar.mysql.MySQLSchema.MySQLTables;
import Sonar.mysql.MySQLVisitor;
import Sonar.mysql.ast.MySQLColumnReference;
import Sonar.mysql.ast.MySQLConstant;
import Sonar.mysql.ast.MySQLExpression;
import Sonar.mysql.ast.MySQLSelect;
import Sonar.mysql.ast.MySQLTableReference;
import Sonar.mysql.ast.MySQLUnaryPostfixOperation;
import Sonar.mysql.ast.MySQLUnaryPostfixOperation.UnaryPostfixOperator;
import Sonar.mysql.ast.MySQLUnaryPrefixOperation;
import Sonar.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;
import Sonar.mysql.gen.MySQLExpressionGenerator;

public class MySQLPivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<MySQLGlobalState, MySQLRowValue, MySQLExpression, SQLConnection> {

    private List<MySQLExpression> fetchColumns;
    private List<MySQLColumn> columns;

    public MySQLPivotedQuerySynthesisOracle(MySQLGlobalState globalState) throws SQLException {
        super(globalState);
        MySQLErrors.addExpressionErrors(errors);
        errors.add("in 'order clause'");
    }

    @Override
    public Query<SQLConnection> getRectifiedQuery() throws SQLException {
        MySQLTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();
        List<MySQLTable> tables = randomFromTables.getTables();

        MySQLSelect selectStatement = new MySQLSelect();
        selectStatement.setSelectType(Randomly.fromOptions(MySQLSelect.SelectType.values()));
        columns = randomFromTables.getColumns();
        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection());

        selectStatement.setFromList(tables.stream().map(t -> new MySQLTableReference(t)).collect(Collectors.toList()));

        fetchColumns = columns.stream().map(c -> new MySQLColumnReference(c, null)).collect(Collectors.toList());
        selectStatement.setFetchColumns(fetchColumns);
        MySQLExpression whereClause = generateRectifiedExpression(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
        List<MySQLExpression> groupByClause = generateGroupByClause(columns, pivotRow);
        selectStatement.setGroupByExpressions(groupByClause);
        MySQLExpression limitClause = generateLimit();
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            MySQLExpression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        List<String> modifiers = Randomly.subset("STRAIGHT_JOIN", "SQL_SMALL_RESULT", "SQL_BIG_RESULT", "SQL_NO_CACHE");
        selectStatement.setModifiers(modifiers);
        List<MySQLExpression> orderBy = new MySQLExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBys();
        selectStatement.setOrderByExpressions(orderBy);

        return new SQLQueryAdapter(MySQLVisitor.asString(selectStatement), errors);
    }

    private List<MySQLExpression> generateGroupByClause(List<MySQLColumn> columns, MySQLRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> MySQLColumnReference.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private MySQLConstant generateLimit() {
        if (Randomly.getBoolean()) {
            return MySQLConstant.createIntConstant(Integer.MAX_VALUE);
        } else {
            return null;
        }
    }

    private MySQLExpression generateOffset() {
        if (Randomly.getBoolean()) {
            return MySQLConstant.createIntConstantNotAsBoolean(0);
        } else {
            return null;
        }
    }

    private MySQLExpression generateRectifiedExpression(List<MySQLColumn> columns, MySQLRowValue rw) {
        MySQLExpression expression = new MySQLExpressionGenerator(globalState).setRowVal(rw).setColumns(columns)
                .generateExpression();
        MySQLConstant expectedValue = expression.getExpectedValue();
        MySQLExpression result;
        if (expectedValue.isNull()) {
            result = new MySQLUnaryPostfixOperation(expression, UnaryPostfixOperator.IS_NULL, false);
        } else if (expectedValue.asBooleanNotNull()) {
            result = expression;
        } else {
            result = new MySQLUnaryPrefixOperation(expression, MySQLUnaryPrefixOperator.NOT);
        }
        rectifiedPredicates.add(result);
        return result;
    }

    @Override
    protected Query<SQLConnection> getContainmentCheckQuery(Query<?> query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM (");
        sb.append(query.getUnterminatedQueryString());
        sb.append(") as result WHERE ");
        int i = 0;
        for (MySQLColumn c : columns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            sb.append("result.");
            sb.append("ref");
            sb.append(i - 1);
            if (pivotRow.getValues().get(c).isNull()) {
                sb.append(" IS NULL");
            } else {
                sb.append(" = ");
                sb.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }

        String resultingQueryString = sb.toString();
        return new SQLQueryAdapter(resultingQueryString, query.getExpectedErrors());
    }

    @Override
    protected String getExpectedValues(MySQLExpression expr) {
        return null;
    }
}
