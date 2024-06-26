package Sonar.mysql.oracle;


import Sonar.ComparatorHelper;
import Sonar.common.oracle.NoRECBase;
import Sonar.common.oracle.TestOracle;
import Sonar.mysql.MySQLErrors;
import Sonar.mysql.MySQLGlobalState;
import Sonar.Randomly;
import Sonar.mysql.MySQLSchema;
import Sonar.mysql.MySQLVisitor;
import Sonar.mysql.ast.*;
import Sonar.mysql.gen.MySQLExpressionGenerator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class MySQLSonarOracle extends NoRECBase<MySQLGlobalState> implements TestOracle<MySQLGlobalState> {

    MySQLSchema s;
    MySQLSchema.MySQLTables targetTables;
    MySQLExpressionGenerator gen;
    MySQLSelect select;


    public MySQLSonarOracle(MySQLGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        MySQLErrors.addExpressionErrors(errors);

        errors.add("Data truncation");
        errors.add("Incorrect DATETIME value");
        errors.add("Incorrect DATE value");
        errors.add("Invalid use of group function");
        errors.add("incompatible with sql_mode=only_full_group_by");
        errors.add("Cannot convert string");

    }

    List<MySQLExpression> generateFetchColumns() {
        List<MySQLExpression> list = new ArrayList<>();
        MySQLColumnReference mySQLColumnReference = MySQLColumnReference.create(targetTables.getColumns().get(0), null);
        String alias = "_" + mySQLColumnReference.getColumn().getColumnName();
        list.add(new MySQLPostfixText(mySQLColumnReference, alias));
        return list;
    }


    List<MySQLExpression> generateGroupByColumns() {
        List<MySQLExpression> list = new ArrayList<>();
        MySQLColumnReference mySQLColumnReference = MySQLColumnReference.create(targetTables.getColumns().get(0), null);
        list.add(mySQLColumnReference);
        return list;
    }


    @Override
    public void check() throws SQLException {
        boolean useFetchColumnsExp = Randomly.getBoolean();
        MySQLPostfixText asText = null;
        MySQLExpression whereExp = null;
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        gen = new MySQLExpressionGenerator(state).setColumns(targetTables.getColumns());
        select = new MySQLSelect();


        if (useFetchColumnsExp) {
            if (Randomly.getBoolean()) {

                MySQLExpression exp = gen.generateWindowFuc();
                asText = new MySQLPostfixText(exp, "f1");
                List<MySQLExpression> fetchColumns = new ArrayList<>();
                fetchColumns.add(asText);
                select.setFetchColumns(fetchColumns);
            } else {
                MySQLExpression exp = gen.generateFetchColumnExpression(targetTables);

                asText = new MySQLPostfixText(exp, "f1");
                List<MySQLExpression> fetchColumns = new ArrayList<>();
                fetchColumns.add(asText);
                select.setFetchColumns(fetchColumns);
            }
        } else {
            select.setFetchColumns(generateFetchColumns());
        }

        select.setJoinList(gen.getRandomJoinClauses(targetTables.getTables()));

        List<MySQLSchema.MySQLTable> tables = targetTables.getTables();
        List<MySQLExpression> tableList = tables.stream().map(t -> new MySQLTableReference(t))
                .collect(Collectors.toList());
        select.setFromList(tableList);

        if (useFetchColumnsExp) {
            whereExp = gen.generateWhereColumnExpression(asText);
            optimizedQueryString = getOptimizedQuery(select, asText, whereExp);
        } else {
            select.setWhereClause(gen.generateExpression());
            optimizedQueryString = getOptimizedQuery(select);
        }

        unoptimizedQueryString = getUnoptimizedQuery(select, useFetchColumnsExp, asText, whereExp);
        List<String> optimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(optimizedQueryString, errors, state);
        List<String> unoptimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(unoptimizedQueryString, errors, state);

        ComparatorHelper.assumeResultSetsAreEqual(optimizedResultSet, unoptimizedResultSet, optimizedQueryString, unoptimizedQueryString, state);

    }


    @Override
    public String getLastQueryString() {
        return optimizedQueryString;
    }

    private String getUnoptimizedQuery(MySQLSelect select, Boolean useFetchColumnsExp, MySQLPostfixText asText, MySQLExpression whereExp) throws SQLException {



        if (useFetchColumnsExp && whereExp != null) {
            if (whereExp instanceof MySQLManuelPredicate) {

                String flag_name = "(" + MySQLVisitor.asString(((MySQLPostfixText) select.getFetchColumns().get(0)).getExpr()) + ")" + " IS TRUE AS flag";
                select.getFetchColumns().add(new MySQLManuelPredicate(flag_name));

            } else {
                MySQLExpression right = null;
                if (whereExp instanceof MySQLBinaryOperation) {

                    right = ((MySQLBinaryOperation) whereExp).getRight();
                    MySQLBinaryOperation.MySQLBinaryOperator op = ((MySQLBinaryOperation) whereExp).getOp();
                    MySQLBinaryOperation fetch_column = new MySQLBinaryOperation(((MySQLPostfixText) select.getFetchColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + MySQLVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getFetchColumns().add(new MySQLManuelPredicate(flag_name));

                } else if (whereExp instanceof MySQLBinaryComparisonOperation) {
                    right = ((MySQLBinaryComparisonOperation) whereExp).getRight();
                    MySQLBinaryComparisonOperation.BinaryComparisonOperator op = ((MySQLBinaryComparisonOperation) whereExp).getOp();
                    MySQLBinaryComparisonOperation fetch_column = new MySQLBinaryComparisonOperation(((MySQLPostfixText) select.getFetchColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + MySQLVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getFetchColumns().add(new MySQLManuelPredicate(flag_name));
                }
            }


            MySQLSelect outerSelect = new MySQLSelect();
            outerSelect.setFetchColumns(Arrays.asList(new MySQLManuelPredicate(asText.getText())));
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new MySQLManuelPredicate("flag=1"));
            unoptimizedQueryString = MySQLVisitor.asString(outerSelect);
            return unoptimizedQueryString;

        } else {


            MySQLSelect outerSelect = new MySQLSelect();
            outerSelect.setFetchColumns(Arrays.asList(new MySQLManuelPredicate(((MySQLPostfixText) select.getFetchColumns().get(0)).getText())));


            String flag_name = "(" + MySQLVisitor.asString(select.getWhereClause()) + ")" + " IS TRUE AS flag";
            select.getFetchColumns().add(new MySQLManuelPredicate(flag_name));


            select.setWhereClause(null);
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new MySQLManuelPredicate("flag=1"));
            unoptimizedQueryString = MySQLVisitor.asString(outerSelect);
            return unoptimizedQueryString;
        }

    }

    private String getOptimizedQuery(MySQLSelect select) throws SQLException {
        if (Randomly.getBoolean()) {
            select.setOrderByExpressions(gen.generateExpressions(1));
        }



        optimizedQueryString = MySQLVisitor.asString(select);
        return optimizedQueryString;
    }


    private String getOptimizedQuery(MySQLSelect select, MySQLPostfixText asText, MySQLExpression whereExp) throws SQLException {




        MySQLSelect outerSelect = new MySQLSelect();
        outerSelect.setFetchColumns(Arrays.asList(new MySQLManuelPredicate(asText.getText())));
        outerSelect.setFromList(Arrays.asList(select));
        outerSelect.setWhereClause(whereExp);

        optimizedQueryString = MySQLVisitor.asString(outerSelect);
        return optimizedQueryString;
    }
}
