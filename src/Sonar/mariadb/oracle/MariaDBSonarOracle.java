package Sonar.mariadb.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.ComparatorHelper;
import Sonar.Randomly;
import Sonar.common.oracle.NoRECBase;
import Sonar.common.oracle.TestOracle;
import Sonar.mariadb.MariaDBProvider.MariaDBGlobalState;
import Sonar.mariadb.MariaDBSchema;
import Sonar.mariadb.MariaDBSchema.MariaDBColumn;
import Sonar.mariadb.ast.*;
import Sonar.mariadb.gen.MariaDBExpressionGenerator;
import Sonar.mariadb.ast.MariaDBExpression;
import Sonar.mariadb.ast.MariaDBSelectStatement;


public class MariaDBSonarOracle extends NoRECBase<MariaDBGlobalState> implements TestOracle<MariaDBGlobalState> {

    private final MariaDBSchema s;
    private static final int NOT_FOUND = -1;
    MariaDBSelectStatement select;

    public MariaDBSonarOracle(MariaDBGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        errors.add("is out of range");

        errors.add("unmatched parentheses");
        errors.add("nothing to repeat at offset");
        errors.add("missing )");
        errors.add("missing terminating ]");
        errors.add("range out of order in character class");
        errors.add("unrecognized character after ");
        errors.add("Got error '(*VERB) not recognized or malformed");
        errors.add("must be followed by");
        errors.add("malformed number or name after");
        errors.add("digit expected after");
    }
    List<MariaDBExpression> generateFetchColumns(List<MariaDBColumn> columns) {
        List<MariaDBExpression> list = new ArrayList<>();
        MariaDBColumnName mariaDBColumnName = new MariaDBColumnName(Randomly.fromList(columns));
        String alias = "_" + mariaDBColumnName.getColumn().getColumnName();
        list.add(new MariaDBPostfixText(mariaDBColumnName, alias));
        return list;
    }

    @Override
    public void check() throws SQLException {

        boolean useFetchColumnsExp = Randomly.getBoolean();

        MariaDBPostfixText asText = null;
        MariaDBExpression whereExp = null;



        MariaDBSchema.MariaDBTables randomTables = s.getRandomTableNonEmptyTables();
        List<MariaDBColumn> columns = randomTables.getColumns();
        MariaDBExpressionGenerator gen = new MariaDBExpressionGenerator(state.getRandomly()).setColumns(columns)
                .setCon(con).setState(state.getState());
        select = new MariaDBSelectStatement();



        if (useFetchColumnsExp) {
            MariaDBExpression exp = gen.generateFetchColumnExpression();
            asText = new MariaDBPostfixText(exp, "f1");
            List<MariaDBExpression> fetchColumns = new ArrayList<>();
            fetchColumns.add(asText);
            select.setFetchColumns(fetchColumns);

        } else {
            select.setFetchColumns(generateFetchColumns(columns));
        }

        select.setJoinList(gen.getRandomJoinClauses(randomTables.getTables()));

        List<MariaDBSchema.MariaDBTable> tables = randomTables.getTables();
        List<MariaDBExpression> tableList = tables.stream().map(t -> new MariaDBTableReference(t))
                .collect(Collectors.toList());
        select.setFromTables(tableList);


        if (useFetchColumnsExp) {
            whereExp = gen.generateWhereColumnExpression(asText);
            optimizedQueryString = getOptimizedQuery(select, asText, whereExp);
        } else {
            select.setWhereClause(gen.getRandomExpression());
            optimizedQueryString = getOptimizedQuery(select);
        }

        unoptimizedQueryString = getUnoptimizedQuery(select, useFetchColumnsExp, asText, whereExp);
        List<String> optimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(optimizedQueryString, errors, state);
        List<String> unoptimizedResultSet = ComparatorHelper.getResultSetFirstColumnAsString(unoptimizedQueryString, errors, state);

        ComparatorHelper.assumeResultSetsAreEqual(optimizedResultSet, unoptimizedResultSet, optimizedQueryString, unoptimizedQueryString, state);















    }

    private String getUnoptimizedQuery(MariaDBSelectStatement select, Boolean useFetchColumnsExp, MariaDBPostfixText asText, MariaDBExpression whereExp) throws SQLException {



        if (useFetchColumnsExp && whereExp != null) {
            if (whereExp instanceof MariaDBManuelPredicate) {

                String flag_name = "(" + MariaDBVisitor.asString(((MariaDBPostfixText) select.getColumns().get(0)).getExpr()) + ")" + " IS TRUE AS flag";
                select.getColumns().add(new MariaDBManuelPredicate(flag_name));

            } else {
                MariaDBExpression right = null;
                if (whereExp instanceof MariaDBBinaryOperator) {
                    right = ((MariaDBBinaryOperator) whereExp).getRight();
                    MariaDBBinaryOperator.MariaDBBinaryComparisonOperator op = ((MariaDBBinaryOperator) whereExp).getOp();
                    MariaDBBinaryOperator fetch_column = new MariaDBBinaryOperator(((MariaDBPostfixText) select.getColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + MariaDBVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getColumns().add(new MariaDBManuelPredicate(flag_name));

                } else{
                    throw new AssertionError(whereExp.getClass().toString());
                }
            }


            MariaDBSelectStatement outerSelect = new MariaDBSelectStatement();
            outerSelect.setFetchColumns(Arrays.asList(new MariaDBManuelPredicate(asText.getText())));
            outerSelect.setFromTables(Arrays.asList(select));
            outerSelect.setWhereClause(new MariaDBManuelPredicate("flag=1"));
            unoptimizedQueryString = MariaDBVisitor.asString(outerSelect);
            return unoptimizedQueryString;

        } else {


            MariaDBSelectStatement outerSelect = new MariaDBSelectStatement();
            outerSelect.setFetchColumns(Arrays.asList(new MariaDBManuelPredicate(((MariaDBPostfixText) select.getColumns().get(0)).getText())));


            String flag_name = "(" + MariaDBVisitor.asString(select.getWhereCondition()) + ")" + " IS TRUE AS flag";
            select.getColumns().add(new MariaDBManuelPredicate(flag_name));

            select.setWhereClause(null);
            outerSelect.setFromTables(Arrays.asList(select));
            outerSelect.setWhereClause(new MariaDBManuelPredicate("flag=1"));
            unoptimizedQueryString = MariaDBVisitor.asString(outerSelect);
            return unoptimizedQueryString;
        }

    }
    
    private String getOptimizedQuery(MariaDBSelectStatement select, MariaDBPostfixText asText, MariaDBExpression whereExp) throws SQLException {

        MariaDBSelectStatement outerSelect = new MariaDBSelectStatement();
        outerSelect.setFetchColumns(Arrays.asList(new MariaDBManuelPredicate(asText.getText())));
        outerSelect.setFromTables(Arrays.asList(select));
        outerSelect.setWhereClause(whereExp);

        optimizedQueryString = MariaDBVisitor.asString(outerSelect);
        return optimizedQueryString;
    }

    private String getOptimizedQuery(MariaDBSelectStatement select) throws SQLException {




        optimizedQueryString = MariaDBVisitor.asString(select);
        return optimizedQueryString;

    }

}
