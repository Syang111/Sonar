package Sonar.postgres.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.ComparatorHelper;
import Sonar.Randomly;

import Sonar.common.oracle.NoRECBase;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.common.query.SonarResultSet;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema;
import Sonar.postgres.PostgresSchema.PostgresColumn;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.PostgresSchema.PostgresTable;
import Sonar.postgres.PostgresSchema.PostgresTables;
import Sonar.postgres.PostgresVisitor;
import Sonar.postgres.ast.*;
import Sonar.postgres.ast.PostgresJoin.PostgresJoinType;
import Sonar.postgres.ast.PostgresSelect.PostgresFromTable;
import Sonar.postgres.ast.PostgresSelect.PostgresSubquery;
import Sonar.postgres.gen.PostgresCommon;
import Sonar.postgres.gen.PostgresExpressionGenerator;
import Sonar.postgres.oracle.tlp.PostgresTLPBase;

public class PostgresSonarOracle extends NoRECBase<PostgresGlobalState> implements TestOracle<PostgresGlobalState> {

    private final PostgresSchema s;

    public PostgresSonarOracle(PostgresGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonFetchErrors(errors);
    }


    List<PostgresExpression> generateFetchColumns(List<PostgresSchema.PostgresColumn> columns) {
        List<PostgresExpression> list = new ArrayList<>();
        String alias = "_" + columns.get(0).getColumnName();
        list.add(new PostgresPostfixText(new PostgresColumnValue(columns.get(0),null), alias,null,null));
        return list;
    }
    

    @Override
    public void check() throws SQLException {

        boolean useFetchColumnsExp = Randomly.getBoolean();

        PostgresPostfixText asText = null;
        PostgresExpression whereExp = null;

        PostgresTables randomTables = s.getRandomTableNonEmptyTables();
        List<PostgresColumn> columns = randomTables.getColumns();

        List<PostgresTable> tables = randomTables.getTables();
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(state).setColumns(columns);


        List<PostgresJoin> joinStatements = getJoinStatements(state, columns, tables);
        List<PostgresExpression> fromTables = tables.stream().map(t -> new PostgresFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList());

        PostgresSelect select = new PostgresSelect();

        if (useFetchColumnsExp) {
            PostgresExpression exp = gen.generateFetchColumnExpression(columns.get(0));
            asText = new PostgresPostfixText(exp, "f1",null,null);
            List<PostgresExpression> fetchColumns = new ArrayList<>();
            fetchColumns.add(asText);
            select.setFetchColumns(fetchColumns);

        } else {
            select.setFetchColumns(generateFetchColumns(columns));
        }

        select.setJoinClauses(joinStatements);
        select.setFromList(fromTables);

        if (useFetchColumnsExp) {

            whereExp = gen.generateWhereColumnExpression(asText,columns.get(0));
            optimizedQueryString = getOptimizedQuery(select, asText, whereExp);
        } else {
            PostgresExpression whereCondition = gen.generateExpression(PostgresDataType.BOOLEAN);
            select.setWhereClause(whereCondition);
            optimizedQueryString = PostgresVisitor.asString(select);
        }

        unoptimizedQueryString = getUnoptimizedQuery(select, useFetchColumnsExp, asText, whereExp);
        List<String> optimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(optimizedQueryString, errors, state);
        List<String> unoptimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(unoptimizedQueryString, errors, state);

        ComparatorHelper.assumeResultSetsAreEqual(optimizedResultSet, unoptimizedResultSet, optimizedQueryString, unoptimizedQueryString, state);

    }

    private PostgresExpression getRandomWhereCondition(List<PostgresColumn> columns) {
        return new PostgresExpressionGenerator(state).setColumns(columns).generateExpression(PostgresDataType.BOOLEAN);
    }

    public static List<PostgresJoin> getJoinStatements(PostgresGlobalState globalState, List<PostgresColumn> columns,
                                                       List<PostgresTable> tables) {
        List<PostgresJoin> joinStatements = new ArrayList<>();
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(globalState).setColumns(columns);
        for (int i = 1; i < tables.size(); i++) {
            PostgresExpression joinClause = gen.generateExpression(PostgresDataType.BOOLEAN);
            PostgresTable table = Randomly.fromList(tables);
            tables.remove(table);
            PostgresJoinType options = PostgresJoinType.getRandom();
            PostgresJoin j = new PostgresJoin(new PostgresFromTable(table, Randomly.getBoolean()), joinClause, options);
            joinStatements.add(j);
        }

        for (int i = 0; i < Randomly.smallNumber(); i++) {
            PostgresTables subqueryTables = globalState.getSchema().getRandomTableNonEmptyTables();
            PostgresSubquery subquery = PostgresTLPBase.createSubquery(globalState, String.format("sub%d", i),
                    subqueryTables);
            PostgresExpression joinClause = gen.generateExpression(PostgresDataType.BOOLEAN);
            PostgresJoinType options = PostgresJoinType.getRandom();
            PostgresJoin j = new PostgresJoin(subquery, joinClause, options);
            joinStatements.add(j);
        }
        return joinStatements;
    }

    private String getOptimizedQuery(PostgresSelect select, PostgresPostfixText asText, PostgresExpression whereExp) throws SQLException {

        PostgresSelect outerSelect = new PostgresSelect();
        outerSelect.setFetchColumns(Arrays.asList(new PostgresManuelPredicate(asText.getText())));
        outerSelect.setFromList(Arrays.asList(select));
        outerSelect.setWhereClause(whereExp);
        optimizedQueryString = PostgresVisitor.asString(outerSelect);
        return optimizedQueryString;
    }

    private String getUnoptimizedQuery(PostgresSelect select, Boolean useFetchColumnsExp, PostgresPostfixText asText,
                                       PostgresExpression whereExp) throws SQLException {
        


        if (useFetchColumnsExp && whereExp != null) {
            if (whereExp instanceof PostgresManuelPredicate) {

                String flag_name = "(" + PostgresVisitor.asString(((PostgresPostfixText) select.getFetchColumns().get(0)).getExpr()) + ")" + " IS TRUE AS flag";
                select.getFetchColumns().add(new PostgresManuelPredicate(flag_name));

            } else {
                PostgresExpression right = null;
                if (whereExp instanceof PostgresBinaryArithmeticOperation) {

                    right = ((PostgresBinaryArithmeticOperation) whereExp).getRight();
                    PostgresBinaryArithmeticOperation.PostgresBinaryOperator op =
                            ((PostgresBinaryArithmeticOperation) whereExp).getOp();
                    PostgresBinaryArithmeticOperation fetch_column = new PostgresBinaryArithmeticOperation(((PostgresPostfixText) select.getFetchColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + PostgresVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getFetchColumns().add(new PostgresManuelPredicate(flag_name));

                } else{
                    throw new AssertionError(whereExp.getClass().toString());
                }
            }


            PostgresSelect outerSelect = new PostgresSelect();
            outerSelect.setFetchColumns(Arrays.asList(new PostgresManuelPredicate(asText.getText())));
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new PostgresManuelPredicate("flag"));
            unoptimizedQueryString = PostgresVisitor.asString(outerSelect);
            return unoptimizedQueryString;

        } else {


            PostgresSelect outerSelect = new PostgresSelect();
            outerSelect.setFetchColumns(Arrays.asList(new PostgresManuelPredicate(((PostgresPostfixText) select.getFetchColumns().get(0)).getText())));


            String flag_name = "(" + PostgresVisitor.asString(select.getWhereClause()) + ")" + " IS TRUE AS flag";
            select.getFetchColumns().add(new PostgresManuelPredicate(flag_name));

            select.setWhereClause(null);
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new PostgresManuelPredicate("flag"));
            unoptimizedQueryString = PostgresVisitor.asString(outerSelect);
            return unoptimizedQueryString;
        }

    }


}

