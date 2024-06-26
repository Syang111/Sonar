package Sonar.tidb.oracle;

import Sonar.ComparatorHelper;
import Sonar.Randomly;
import Sonar.common.oracle.NoRECBase;
import Sonar.common.oracle.TestOracle;
import Sonar.tidb.TiDBErrors;
import Sonar.tidb.TiDBExpressionGenerator;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.TiDBSchema;
import Sonar.tidb.TiDBSchema.TiDBColumn;
import Sonar.tidb.TiDBSchema.TiDBTable;
import Sonar.tidb.TiDBSchema.TiDBTables;
import Sonar.tidb.ast.*;
import Sonar.tidb.visitor.TiDBVisitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TiDBSonarOracle extends NoRECBase<TiDBGlobalState> implements TestOracle<TiDBGlobalState> {
    TiDBSchema s;
    TiDBSchema.TiDBTables targetTables;



    public TiDBSonarOracle(TiDBGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        TiDBErrors.addExpressionErrors(errors);

    }

    List<TiDBExpression> generateFetchColumns(List<TiDBSchema.TiDBColumn> columns) {
        List<TiDBExpression> list = new ArrayList<>();
        TiDBColumnReference TiDBColumnName = new TiDBColumnReference(Randomly.fromList(columns));
        String alias = "_" + TiDBColumnName.getColumn().getColumnName();
        list.add(new TiDBPostfixText(TiDBColumnName, alias));
        return list;
    }

    public void check() throws SQLException {
        boolean useFetchColumnsExp = Randomly.getBoolean();

        TiDBPostfixText asText = null;
        TiDBExpression whereExp = null;
        TiDBTables randomTables = s.getRandomTableNonEmptyTables();
        List<TiDBColumn> columns = randomTables.getColumns();
        TiDBExpressionGenerator gen = new TiDBExpressionGenerator(this.state).setColumns(columns);
        TiDBSelect select = new TiDBSelect();



        if (useFetchColumnsExp) {
            TiDBExpression exp = gen.generateFetchColumnExpression(columns.get(0));
            asText = new TiDBPostfixText(exp, "f1");
            List<TiDBExpression> fetchColumns = new ArrayList<>();
            fetchColumns.add(asText);
            select.setFetchColumns(fetchColumns);
        } else {
            select.setFetchColumns(generateFetchColumns(columns));
        }




        List<TiDBTable> tables = randomTables.getTables();
        List<TiDBExpression> tableList = tables.stream().map(t -> new TiDBTableReference(t))
                .collect(Collectors.toList());
        select.setFromList(tableList);
        List<TiDBExpression> joins = TiDBJoin.getJoins(tableList, state);
        select.setJoinList(joins);


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

    private String getUnoptimizedQuery(TiDBSelect select, Boolean
            useFetchColumnsExp, TiDBPostfixText asText, TiDBExpression whereExp) throws SQLException {
        


        if (useFetchColumnsExp && whereExp != null) {
            if (whereExp instanceof TiDBManuelPredicate) {

                String flag_name = "(" + TiDBVisitor.asString(((TiDBPostfixText) select.getFetchColumns().get(0)).getExpr()) + ")" + " IS TRUE AS flag";
                select.getFetchColumns().add(new TiDBManuelPredicate(flag_name));

            } else {
                TiDBExpression right = null;
                if (whereExp instanceof TiDBBinaryArithmeticOperation) {
                    right = ((TiDBBinaryArithmeticOperation) whereExp).getRight();
                    TiDBBinaryArithmeticOperation.TiDBBinaryArithmeticOperator op = ((TiDBBinaryArithmeticOperation) whereExp).getOp();
                    TiDBBinaryArithmeticOperation fetch_column = new TiDBBinaryArithmeticOperation(((TiDBPostfixText) select.getFetchColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + TiDBVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getFetchColumns().add(new TiDBManuelPredicate(flag_name));

                } else if(whereExp instanceof TiDBBinaryComparisonOperation) {
                    right = ((TiDBBinaryComparisonOperation) whereExp).getRight();
                    TiDBBinaryComparisonOperation.TiDBComparisonOperator op = ((TiDBBinaryComparisonOperation) whereExp).getOp();
                    TiDBBinaryComparisonOperation fetch_column = new TiDBBinaryComparisonOperation(((TiDBPostfixText) select.getFetchColumns().get(0)).getExpr(), right, op);
                    String flag_name = "(" + TiDBVisitor.asString(fetch_column) + ")" + " IS TRUE AS flag";
                    select.getFetchColumns().add(new TiDBManuelPredicate(flag_name));
                } else{
                    throw new AssertionError(whereExp.getClass().toString());
                }
            }


            TiDBSelect outerSelect = new TiDBSelect();
            outerSelect.setFetchColumns(Arrays.asList(new TiDBManuelPredicate(asText.getText())));
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new TiDBManuelPredicate("flag=1"));
            unoptimizedQueryString = TiDBVisitor.asString(outerSelect);
            return unoptimizedQueryString;

        } else {


            TiDBSelect outerSelect = new TiDBSelect();
            outerSelect.setFetchColumns(Arrays.asList(new TiDBManuelPredicate(((TiDBPostfixText) select.getFetchColumns().get(0)).getText())));


            String flag_name = "(" + TiDBVisitor.asString(select.getWhereClause()) + ")" + " IS TRUE AS flag";
            select.getFetchColumns().add(new TiDBManuelPredicate(flag_name));

            select.setWhereClause(null);
            outerSelect.setFromList(Arrays.asList(select));
            outerSelect.setWhereClause(new TiDBManuelPredicate("flag=1"));
            unoptimizedQueryString = TiDBVisitor.asString(outerSelect);
            return unoptimizedQueryString;
        }

    }

    private String getOptimizedQuery(TiDBSelect select, TiDBPostfixText asText, TiDBExpression
            whereExp) throws SQLException {

        TiDBSelect outerSelect = new TiDBSelect();
        outerSelect.setFetchColumns(Arrays.asList(new TiDBManuelPredicate(asText.getText())));
        outerSelect.setFromList(Arrays.asList(select));
        outerSelect.setWhereClause(whereExp);
        optimizedQueryString = TiDBVisitor.asString(outerSelect);
        return optimizedQueryString;
    }

    private String getOptimizedQuery(TiDBSelect select) throws SQLException {




        optimizedQueryString = TiDBVisitor.asString(select);
        return optimizedQueryString;

    }
}
