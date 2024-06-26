package Sonar.sqlite3.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.SQLConnection;
import Sonar.StateToReproduce.OracleRunReproductionState;
import Sonar.common.oracle.PivotedQuerySynthesisBase;
import Sonar.common.query.Query;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3Errors;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.SQLite3Visitor;
import Sonar.sqlite3.ast.SQLite3Aggregate;
import Sonar.sqlite3.ast.SQLite3Aggregate.SQLite3AggregateFunction;
import Sonar.sqlite3.ast.SQLite3Cast;
import Sonar.sqlite3.ast.SQLite3Constant;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Expression.Join;
import Sonar.sqlite3.ast.SQLite3Expression.Join.JoinType;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3Distinct;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixText;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixUnaryOperation;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixUnaryOperation.PostfixUnaryOperator;
import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.ast.SQLite3UnaryOperation;
import Sonar.sqlite3.ast.SQLite3UnaryOperation.UnaryOperator;
import Sonar.sqlite3.ast.SQLite3WindowFunction;
import Sonar.sqlite3.gen.SQLite3Common;
import Sonar.sqlite3.gen.SQLite3ExpressionGenerator;
import Sonar.sqlite3.schema.SQLite3Schema;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Column;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3RowValue;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Table;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Tables;

public class SQLite3PivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<SQLite3GlobalState, SQLite3RowValue, SQLite3Expression, SQLConnection> {

    private List<SQLite3Column> fetchColumns;
    private OracleRunReproductionState localState;

    public SQLite3PivotedQuerySynthesisOracle(SQLite3GlobalState globalState) {
        super(globalState);
    }

    @Override
    public Query<SQLConnection> getRectifiedQuery() throws SQLException {
        SQLite3Select selectStatement = getQuery();
        SQLite3Errors.addExpectedExpressionErrors(errors);
        return new SQLQueryAdapter(SQLite3Visitor.asString(selectStatement), errors);
    }

    public SQLite3Select getQuery() throws SQLException {
        assert !globalState.getSchema().getDatabaseTables().isEmpty();
        localState = globalState.getState().getLocalState();
        assert localState != null;
        SQLite3Tables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();
        List<SQLite3Table> tables = randomFromTables.getTables();

        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection());
        SQLite3Select selectStatement = new SQLite3Select();
        selectStatement.setSelectType(Randomly.fromOptions(SQLite3Select.SelectType.values()));
        List<SQLite3Column> columns = randomFromTables.getColumns();


        List<SQLite3Column> columnsWithoutRowid = columns.stream()
                .filter(c -> !SQLite3Schema.ROWID_STRINGS.contains(c.getName())).collect(Collectors.toList());
        List<Join> joinStatements = getJoinStatements(globalState, tables, columnsWithoutRowid);
        selectStatement.setJoinClauses(joinStatements);
        selectStatement.setFromTables(SQLite3Common.getTableRefs(tables, globalState.getSchema()));

        fetchColumns = Randomly.nonEmptySubset(columnsWithoutRowid);
        List<SQLite3Table> allTables = new ArrayList<>();
        allTables.addAll(tables);
        allTables.addAll(joinStatements.stream().map(join -> join.getTable()).collect(Collectors.toList()));
        boolean allTablesContainOneRow = allTables.stream().allMatch(t -> t.getNrRows(globalState) == 1);
        boolean testAggregateFunctions = allTablesContainOneRow && globalState.getOptions().testAggregateFunctionsPQS();
        pivotRowExpression = getColExpressions(testAggregateFunctions, columnsWithoutRowid);
        selectStatement.setFetchColumns(pivotRowExpression);
        SQLite3Expression whereClause = generateRectifiedExpression(columnsWithoutRowid, pivotRow, false);
        selectStatement.setWhereClause(whereClause);
        List<SQLite3Expression> groupByClause = generateGroupByClause(columnsWithoutRowid, pivotRow,
                allTablesContainOneRow);
        selectStatement.setGroupByClause(groupByClause);
        SQLite3Expression limitClause = generateLimit((long) (Math.pow(globalState.getOptions().getMaxNumberInserts(),
                joinStatements.size() + randomFromTables.getTables().size())));
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            SQLite3Expression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        
        List<SQLite3Expression> orderBy = new SQLite3ExpressionGenerator(globalState).generateOrderBys();
        selectStatement.setOrderByExpressions(orderBy);
        if (!groupByClause.isEmpty() && Randomly.getBoolean()) {
            selectStatement.setHavingClause(generateRectifiedExpression(columns, pivotRow, true));
        }
        return selectStatement;
    }

    private List<Join> getJoinStatements(SQLite3GlobalState globalState, List<SQLite3Table> tables,
            List<SQLite3Column> columns) {
        List<Join> joinStatements = new SQLite3ExpressionGenerator(globalState).getRandomJoinClauses(tables);
        for (Join j : joinStatements) {
            if (j.getType() == JoinType.NATURAL) {
                
                j.setType(JoinType.INNER);
            }

            j.setOnClause(generateRectifiedExpression(columns, pivotRow, false));
        }
        errors.add("ON clause references tables to its right");
        return joinStatements;
    }

    private List<SQLite3Expression> getColExpressions(boolean testAggregateFunctions, List<SQLite3Column> columns) {
        List<SQLite3Expression> colExpressions = new ArrayList<>();

        for (SQLite3Column c : fetchColumns) {
            SQLite3Expression colName = new SQLite3ColumnName(c, pivotRow.getValues().get(c));
            if (testAggregateFunctions && Randomly.getBoolean()) {

                
                boolean generateDistinct = Randomly.getBooleanWithRatherLowProbability();
                if (generateDistinct) {
                    colName = new SQLite3Distinct(colName);
                }

                SQLite3AggregateFunction aggFunc = SQLite3AggregateFunction.getRandom(c.getType());
                colName = new SQLite3Aggregate(Arrays.asList(colName), aggFunc);
                if (Randomly.getBoolean() && !generateDistinct) {
                    colName = generateWindowFunction(columns, colName, true);
                }
                errors.add("second argument to nth_value must be a positive integer");
            }
            if (Randomly.getBoolean()) {
                SQLite3Expression randomExpression;
                randomExpression = new SQLite3ExpressionGenerator(globalState).setColumns(columns)
                        .generateResultKnownExpression();
                colExpressions.add(randomExpression);
            } else {
                colExpressions.add(colName);
            }
        }
        if (testAggregateFunctions) {
            SQLite3WindowFunction windowFunction = SQLite3WindowFunction.getRandom(columns, globalState);
            SQLite3Expression windowExpr = generateWindowFunction(columns, windowFunction, false);
            colExpressions.add(windowExpr);
        }
        for (SQLite3Expression expr : colExpressions) {
            if (expr.getExpectedValue() == null) {
                throw new IgnoreMeException();
            }
        }
        return colExpressions;
    }

    private SQLite3Expression generateOffset() {
        if (Randomly.getBoolean()) {
            return SQLite3Constant.createIntConstant(0);
        } else {
            return null;
        }
    }

    @Override
    protected Query<SQLConnection> getContainmentCheckQuery(Query<?> query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        String checkForContainmentValues = getGeneralizedPivotRowValues();
        sb.append(checkForContainmentValues);
        globalState.getState().getLocalState()
                .log("-- we expect the following expression to be contained in the result set: "
                        + checkForContainmentValues);
        sb.append(" INTERSECT SELECT * FROM (");
        sb.append(query.getUnterminatedQueryString());
        sb.append(")");
        String resultingQueryString = sb.toString();
        return new SQLQueryAdapter(resultingQueryString, query.getExpectedErrors());
    }

    private String getGeneralizedPivotRowValues() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pivotRowExpression.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            SQLite3Constant expectedValue = pivotRowExpression.get(i).getExpectedValue();
            String value = SQLite3Visitor.asString(expectedValue);
            if (value.contains("�") || value.contains("\0")) {

                throw new IgnoreMeException();
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private SQLite3Expression generateLimit(long l) {
        if (Randomly.getBoolean()) {
            return SQLite3Constant.createIntConstant(globalState.getRandomly().getLong(l, Long.MAX_VALUE));
        } else {
            return null;
        }
    }

    private List<SQLite3Expression> generateGroupByClause(List<SQLite3Column> columns, SQLite3RowValue rw,
            boolean allTablesContainOneRow) {
        errors.add("GROUP BY term out of range");
        if (allTablesContainOneRow && Randomly.getBoolean()) {
            List<SQLite3Expression> collect = new ArrayList<>();
            for (int i = 0; i < Randomly.smallNumber(); i++) {
                collect.add(new SQLite3ExpressionGenerator(globalState).setColumns(columns).setRowValue(rw)
                        .generateExpression());
            }
            return collect;
        }
        if (Randomly.getBoolean()) {

            List<SQLite3Expression> collect = columns.stream().map(c -> new SQLite3ColumnName(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
            if (Randomly.getBoolean()) {
                for (int i = 0; i < Randomly.smallNumber(); i++) {
                    collect.add(new SQLite3ExpressionGenerator(globalState).setColumns(columns).setRowValue(rw)
                            .generateExpression());
                }
            }
            return collect;
        } else {
            return Collections.emptyList();
        }
    }

    
    private SQLite3Expression generateRectifiedExpression(List<SQLite3Column> columns, SQLite3RowValue pivotRow,
            boolean allowAggregates) {
        SQLite3ExpressionGenerator gen = new SQLite3ExpressionGenerator(globalState).setRowValue(pivotRow)
                .setColumns(columns);
        if (allowAggregates) {
            gen = gen.allowAggregateFunctions();
        }
        SQLite3Expression expr = gen.generateResultKnownExpression();
        SQLite3Expression rectifiedPredicate;
        if (expr.getExpectedValue().isNull()) {

            rectifiedPredicate = new SQLite3PostfixUnaryOperation(PostfixUnaryOperator.ISNULL, expr);
        } else if (SQLite3Cast.isTrue(expr.getExpectedValue()).get()) {

            rectifiedPredicate = expr;
        } else {

            rectifiedPredicate = new SQLite3UnaryOperation(UnaryOperator.NOT, expr);
        }
        rectifiedPredicates.add(rectifiedPredicate);
        return rectifiedPredicate;
    }


    private SQLite3Expression generateWindowFunction(List<SQLite3Column> columns, SQLite3Expression colName,
            boolean allowFilter) {
        StringBuilder sb = new StringBuilder();
        if (Randomly.getBoolean() && allowFilter) {
            appendFilter(columns, sb);
        }
        sb.append(" OVER ");
        sb.append("(");
        if (Randomly.getBoolean()) {
            appendPartitionBy(columns, sb);
        }
        if (Randomly.getBoolean()) {
            sb.append(SQLite3Common.getOrderByAsString(columns, globalState));
        }
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("RANGE", "ROWS", "GROUPS"));
            sb.append(" ");
            switch (Randomly.fromOptions(FrameSpec.values())) {
            case BETWEEN:
                sb.append("BETWEEN");
                sb.append(" UNBOUNDED PRECEDING AND CURRENT ROW");
                break;
            case UNBOUNDED_PRECEDING:
                sb.append("UNBOUNDED PRECEDING");
                break;
            case CURRENT_ROW:
                sb.append("CURRENT ROW");
                break;
            default:
                throw new AssertionError();
            }
            if (Randomly.getBoolean()) {
                sb.append(" EXCLUDE ");
                sb.append(Randomly.fromOptions("NO OTHERS", "TIES"));
            }
        }
        sb.append(")");
        SQLite3PostfixText windowFunction = new SQLite3PostfixText(colName, sb.toString(), colName.getExpectedValue());
        errors.add("misuse of aggregate");
        return windowFunction;
    }

    private void appendFilter(List<SQLite3Column> columns, StringBuilder sb) {
        sb.append(" FILTER (WHERE ");
        sb.append(SQLite3Visitor.asString(generateRectifiedExpression(columns, pivotRow, false)));
        sb.append(")");
    }

    private void appendPartitionBy(List<SQLite3Column> columns, StringBuilder sb) {
        sb.append(" PARTITION BY ");
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            String orderingTerm;
            do {
                orderingTerm = SQLite3Common.getOrderingTerm(columns, globalState);
            } while (orderingTerm.contains("ASC") || orderingTerm.contains("DESC"));

            sb.append(orderingTerm);
        }
    }

    private enum FrameSpec {
        BETWEEN, UNBOUNDED_PRECEDING, CURRENT_ROW
    }

    @Override
    protected String getExpectedValues(SQLite3Expression expr) {
        return SQLite3Visitor.asExpectedValues(expr);
    }

}
