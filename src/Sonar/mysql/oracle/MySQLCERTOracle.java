package Sonar.mysql.oracle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.SQLGlobalState;
import Sonar.common.DBMSCommon;
import Sonar.common.oracle.CERTOracleBase;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.common.query.SonarResultSet;
import Sonar.mysql.MySQLErrors;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLSchema.MySQLTables;
import Sonar.mysql.MySQLVisitor;
import Sonar.mysql.ast.MySQLBinaryLogicalOperation;
import Sonar.mysql.ast.MySQLBinaryLogicalOperation.MySQLBinaryLogicalOperator;
import Sonar.mysql.ast.MySQLColumnReference;
import Sonar.mysql.ast.MySQLExpression;
import Sonar.mysql.ast.MySQLSelect;
import Sonar.mysql.ast.MySQLTableReference;
import Sonar.mysql.gen.MySQLExpressionGenerator;

public class MySQLCERTOracle extends CERTOracleBase<MySQLGlobalState> implements TestOracle<MySQLGlobalState> {
    private MySQLExpressionGenerator gen;
    private MySQLSelect select;

    public MySQLCERTOracle(MySQLGlobalState globalState) {
        super(globalState);
        MySQLErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        queryPlan1Sequences = new ArrayList<>();
        queryPlan2Sequences = new ArrayList<>();


        MySQLTables tables = state.getSchema().getRandomTableNonEmptyTables();
        gen = new MySQLExpressionGenerator(state).setColumns(tables.getColumns());
        List<MySQLExpression> fetchColumns = new ArrayList<>();
        fetchColumns.addAll(Randomly.nonEmptySubset(tables.getColumns()).stream()
                .map(c -> new MySQLColumnReference(c, null)).collect(Collectors.toList()));
        List<MySQLExpression> tableList = tables.getTables().stream().map(t -> new MySQLTableReference(t))
                .collect(Collectors.toList());

        select = new MySQLSelect();
        select.setFetchColumns(fetchColumns);
        select.setFromList(tableList);

        select.setSelectType(Randomly.fromOptions(MySQLSelect.SelectType.values()));
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(fetchColumns);
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateExpression());
            }
        }






        String queryString1 = MySQLVisitor.asString(select);
        int rowCount1 = getRow(state, queryString1, queryPlan1Sequences);

        boolean increase = mutate(Mutator.JOIN, Mutator.LIMIT);


        String queryString2 = MySQLVisitor.asString(select);
        int rowCount2 = getRow(state, queryString2, queryPlan2Sequences);


        if (DBMSCommon.editDistance(queryPlan1Sequences, queryPlan2Sequences) > 1) {
            return;
        }


        if (increase && rowCount1 > rowCount2 || !increase && rowCount1 < rowCount2) {
            throw new AssertionError("Inconsistent result for query: EXPLAIN " + queryString1 + "; --" + rowCount1
                    + "\nEXPLAIN " + queryString2 + "; --" + rowCount2);
        }
    }

    @Override
    protected boolean mutateDistinct() {
        MySQLSelect.SelectType selectType = select.getFromOptions();
        if (selectType != MySQLSelect.SelectType.ALL) {
            select.setSelectType(MySQLSelect.SelectType.ALL);
            return true;
        } else {
            select.setSelectType(MySQLSelect.SelectType.DISTINCT);
            return false;
        }
    }

    @Override
    protected boolean mutateWhere() {
        boolean increase = select.getWhereClause() != null;
        if (increase) {
            select.setWhereClause(null);
        } else {
            select.setWhereClause(gen.generateExpression());
        }
        return increase;
    }

    @Override
    protected boolean mutateGroupBy() {
        boolean increase = select.getGroupByExpressions().size() > 0;
        if (increase) {
            select.clearGroupByExpressions();
        } else {
            select.setGroupByExpressions(select.getFetchColumns());
        }
        return increase;
    }

    @Override
    protected boolean mutateHaving() {
        if (select.getGroupByExpressions().size() == 0) {
            select.setGroupByExpressions(select.getFetchColumns());
            select.setHavingClause(gen.generateExpression());
            return false;
        } else {
            if (select.getHavingClause() == null) {
                select.setHavingClause(gen.generateExpression());
                return false;
            } else {
                select.setHavingClause(null);
                return true;
            }
        }
    }

    @Override
    protected boolean mutateAnd() {
        if (select.getWhereClause() == null) {
            select.setWhereClause(gen.generateExpression());
        } else {
            MySQLExpression newWhere = new MySQLBinaryLogicalOperation(select.getWhereClause(),
                    gen.generateExpression(), MySQLBinaryLogicalOperator.AND);
            select.setWhereClause(newWhere);
        }
        return false;
    }

    @Override
    protected boolean mutateOr() {
        if (select.getWhereClause() == null) {
            select.setWhereClause(gen.generateExpression());
            return false;
        } else {
            MySQLExpression newWhere = new MySQLBinaryLogicalOperation(select.getWhereClause(),
                    gen.generateExpression(), MySQLBinaryLogicalOperator.OR);
            select.setWhereClause(newWhere);
            return true;
        }
    }












    private int getRow(SQLGlobalState<?, ?> globalState, String selectStr, List<String> queryPlanSequences)
            throws AssertionError, SQLException {
        int row = -1;
        String explainQuery = "EXPLAIN " + selectStr;


        if (globalState.getOptions().logEachSelect()) {
            globalState.getLogger().writeCurrent(explainQuery);
            try {
                globalState.getLogger().getCurrentFileWriter().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        SQLQueryAdapter q = new SQLQueryAdapter(explainQuery, errors);
        try (SonarResultSet rs = q.executeAndGet(globalState)) {
            if (rs != null) {
                while (rs.next()) {
                    int estRows = rs.getInt(10);
                    if (row == -1) {
                        row = estRows;
                    }
                    String operation = rs.getString(2);
                    queryPlanSequences.add(operation);
                }
            }
        } catch (Exception e) {
            throw new AssertionError(q.getQueryString(), e);
        }
        if (row == -1) {
            throw new IgnoreMeException();
        }
        return row;
    }

}
