package Sonar.sqlite3.oracle;


import java.sql.SQLException;
import java.util.*;

import java.util.stream.Collectors;

import Sonar.ComparatorHelper;

import Sonar.Randomly;
import Sonar.Reproducer;
import Sonar.common.oracle.NoRECBase;
import Sonar.common.oracle.TestOracle;

import Sonar.sqlite3.SQLite3Errors;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.SQLite3Visitor;

import Sonar.sqlite3.ast.SQLite3Aggregate;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Expression.Join;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;

import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixText;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ManuelPredicate;

import Sonar.sqlite3.gen.SQLite3Common;
import Sonar.sqlite3.gen.SQLite3ExpressionGenerator;
import Sonar.sqlite3.schema.SQLite3DataType;
import Sonar.sqlite3.schema.SQLite3Schema;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Column;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Table;
import Sonar.sqlite3.schema.SQLite3Schema.SQLite3Tables;


public class SQLite3SonarOracle extends NoRECBase<SQLite3GlobalState> implements TestOracle<SQLite3GlobalState> {


    private final SQLite3Schema s;
    private SQLite3ExpressionGenerator gen;
    private Reproducer<SQLite3GlobalState> reproducer;

    private Boolean useAggre = false;


    public SQLite3SonarOracle(SQLite3GlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        SQLite3Errors.addExpectedExpressionErrors(errors);
        SQLite3Errors.addMatchQueryErrors(errors);
        SQLite3Errors.addQueryErrors(errors);
        errors.add("misuse of aggregate");
        errors.add("misuse of window function");
        errors.add("second argument to nth_value must be a positive integer");
        errors.add("no such table");
        errors.add("no query solution");
        errors.add("unable to use function MATCH in the requested context");
    }

    @Override
    public void check() throws SQLException {
        reproducer = null;
        SQLite3Tables randomTables = s.getRandomTableNonEmptyTables();
        List<SQLite3Column> columns = randomTables.getColumns();
        gen = new SQLite3ExpressionGenerator(state).setColumns(columns);
        SQLite3Expression randomWhereCondition = null;
        List<SQLite3Table> tables = randomTables.getTables();
        List<Join> joinStatements = gen.getRandomJoinClauses(tables);
        List<SQLite3Expression> tableRefs = SQLite3Common.getTableRefs(tables, s);
        List<SQLite3Expression> groupByColumn = Arrays.asList(new SQLite3ColumnName(new SQLite3Column(columns.get(0).getName(), SQLite3DataType.INT, false, false, null), null));

        boolean useColumnExp = Randomly.getBoolean();
        List<SQLite3Expression> fetchColumns = new ArrayList<>();
        if (useColumnExp) {
            SQLite3Expression exp = null;
            useAggre = Randomly.getBoolean();
            if (useAggre) {
                exp = new SQLite3Aggregate(Arrays.asList(gen.getRandomExpression(2)), Randomly.fromOptions(SQLite3Aggregate.SQLite3AggregateFunction.values()));
            } else {
                exp = gen.generateFetchColumnExp(columns.get(0));
            }

            SQLite3Expression.SQLite3PostfixText f1 = new SQLite3Expression.SQLite3PostfixText(exp, "AS f1", null);
            fetchColumns.add(f1);
            randomWhereCondition = gen.generateWhereColumnExpression(new SQLite3Expression.SQLite3ManuelPredicate("f1"));

        } else {
            this.useAggre = false;
            fetchColumns = generateFetchColumns(columns);
            randomWhereCondition = gen.generateExpression();
        }

        SQLite3Select select = new SQLite3Select();
        select.setFetchColumns(fetchColumns);
        select.setFromTables(tableRefs);
        select.setJoinClauses(joinStatements);

        optimizedQueryString = getOptimizedQuery(select, randomWhereCondition, groupByColumn);
        unoptimizedQueryString = getUnoptimizedQuery(select, randomWhereCondition, useColumnExp);

        List<String> optimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(optimizedQueryString, errors, state);
        List<String> unoptimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(unoptimizedQueryString, errors, state);

        ComparatorHelper.assumeResultSetsAreEqual(optimizedResultSet, unoptimizedResultSet, optimizedQueryString, unoptimizedQueryString, state);


    }

    List<SQLite3Expression> generateFetchColumns(List<SQLite3Column> targetColumns) {
        List<SQLite3Expression> columns = new ArrayList<>();
        if (Randomly.getBoolean()) {
            columns.add(new SQLite3ColumnName(SQLite3Column.createDummy("*"), null));
        } else {
            columns = Randomly.nonEmptySubset(targetColumns).stream()
                    .map(c -> new SQLite3ColumnName(c, null)).collect(Collectors.toList());
        }
        return columns;
    }

    @Override
    public Reproducer<SQLite3GlobalState> getLastReproducer() {
        return reproducer;
    }

    @Override
    public String getLastQueryString() {
        return optimizedQueryString;
    }

    private String getUnoptimizedQuery(SQLite3Select select, SQLite3Expression randomWhereCondition, Boolean useColumnExp) throws SQLException {
        

        if (!useColumnExp) {

            List<SQLite3Expression> clonedFetchColumns = select.getFetchColumns().stream().map(c -> new SQLite3ColumnName(((SQLite3ColumnName) c).getColumn(), null)).collect(Collectors.toList());
            String predicate_column = SQLite3Visitor.asString(randomWhereCondition);
            String flag_name = "(" + predicate_column + ")" + " IS TRUE AS flag";
            clonedFetchColumns.add(new SQLite3ColumnName(new SQLite3Column(flag_name, SQLite3DataType.INT, false, false, null), null));


            SQLite3Expression whereCondition = new SQLite3Expression.SQLite3ManuelPredicate("flag=1");


            SQLite3Select subSelect = new SQLite3Select(select);
            subSelect.setFetchColumns(clonedFetchColumns);
            subSelect.setWhereClause(whereCondition);
            List<SQLite3Expression> fromRef = new ArrayList<>();
            fromRef.add(subSelect);


            SQLite3Select unoptimizedSelect = new SQLite3Select();
            unoptimizedSelect.setFetchColumns(select.getFetchColumns());
            unoptimizedSelect.setFromTables(fromRef);
            unoptimizedQueryString = SQLite3Visitor.asString(unoptimizedSelect);
        } else {

            List<SQLite3Expression> fetchColumns = new ArrayList<>();


            fetchColumns.add(select.getFetchColumns().get(0));


            if (randomWhereCondition instanceof SQLite3ManuelPredicate) {
                String flag_name = "(" + SQLite3Visitor.asString(((SQLite3PostfixText) select.getFetchColumns().get(0)).getExpression()) + ")" + " IS TRUE AS flag";
                fetchColumns.add(new SQLite3ColumnName(new SQLite3Column(flag_name, SQLite3DataType.INT, false, false, null), null));
            } else {
                SQLite3Expression right = null;
                if (randomWhereCondition instanceof SQLite3Expression.Sqlite3BinaryOperation) {
                    right = ((SQLite3Expression.Sqlite3BinaryOperation) randomWhereCondition).getRight();
                    SQLite3Expression.Sqlite3BinaryOperation.BinaryOperator op = ((SQLite3Expression.Sqlite3BinaryOperation) randomWhereCondition).getOperator();
                    SQLite3Expression.Sqlite3BinaryOperation exp = new SQLite3Expression.Sqlite3BinaryOperation(((SQLite3PostfixText) select.getFetchColumns().get(0)).getExpression(), right, op);
                    String flag_name = "(" + SQLite3Visitor.asString(exp) + ")" + " IS TRUE AS flag";
                    fetchColumns.add(new SQLite3ManuelPredicate(flag_name));

                } else if (randomWhereCondition instanceof SQLite3Expression.BinaryComparisonOperation) {
                    right = ((SQLite3Expression.BinaryComparisonOperation) randomWhereCondition).getRight();
                    SQLite3Expression.BinaryComparisonOperation.BinaryComparisonOperator op = ((SQLite3Expression.BinaryComparisonOperation) randomWhereCondition).getOperator();
                    SQLite3Expression.BinaryComparisonOperation exp = new SQLite3Expression.BinaryComparisonOperation(((SQLite3PostfixText) select.getFetchColumns().get(0)).getExpression(), right, op);
                    String flag_name = "(" + SQLite3Visitor.asString(exp) + ")" + " IS TRUE AS flag";
                    fetchColumns.add(new SQLite3ManuelPredicate(flag_name));
                }
            }



            SQLite3Expression whereCondition = new SQLite3Expression.SQLite3ManuelPredicate("flag=1");


            SQLite3Select subSelect = new SQLite3Select(select);
            subSelect.setFetchColumns(fetchColumns);

            if (useAggre) {
                subSelect.setHavingClause(whereCondition);
            } else {
                subSelect.setWhereClause(whereCondition);
            }


            List<SQLite3Expression> fromRef = new ArrayList<>();
            fromRef.add(subSelect);


            SQLite3Select unoptimizedSelect = new SQLite3Select();
            unoptimizedSelect.setFetchColumns(Arrays.asList(new SQLite3Expression.SQLite3ManuelPredicate("f1")));
            unoptimizedSelect.setFromTables(fromRef);
            unoptimizedQueryString = SQLite3Visitor.asString(unoptimizedSelect);
        }

        return unoptimizedQueryString;
    }

    private String getOptimizedQuery(SQLite3Select select, SQLite3Expression randomWhereCondition, List<SQLite3Expression> groupByColumn) throws SQLException {



        if (useAggre) {
            select.setHavingClause(randomWhereCondition);
            select.setGroupByClause(groupByColumn);
        } else {
            select.setWhereClause(randomWhereCondition);
        }
        optimizedQueryString = SQLite3Visitor.asString(select);
        return optimizedQueryString;
    }

}

