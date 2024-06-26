package Sonar.tidb.oracle;

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
import Sonar.tidb.TiDBErrors;
import Sonar.tidb.TiDBExpressionGenerator;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.TiDBSchema.TiDBColumn;
import Sonar.tidb.TiDBSchema.TiDBDataType;
import Sonar.tidb.TiDBSchema.TiDBTables;
import Sonar.tidb.ast.TiDBBinaryLogicalOperation;
import Sonar.tidb.ast.TiDBBinaryLogicalOperation.TiDBBinaryLogicalOperator;
import Sonar.tidb.ast.TiDBColumnReference;
import Sonar.tidb.ast.TiDBExpression;
import Sonar.tidb.ast.TiDBJoin;
import Sonar.tidb.ast.TiDBJoin.JoinType;
import Sonar.tidb.ast.TiDBSelect;
import Sonar.tidb.ast.TiDBTableReference;
import Sonar.tidb.visitor.TiDBVisitor;

public class TiDBCERTOracle extends CERTOracleBase<TiDBGlobalState> implements TestOracle<TiDBGlobalState> {
    private TiDBExpressionGenerator gen;
    private TiDBSelect select;

    public TiDBCERTOracle(TiDBGlobalState globalState) {
        super(globalState);
        TiDBErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        queryPlan1Sequences = new ArrayList<>();
        queryPlan2Sequences = new ArrayList<>();


        TiDBTables tables = state.getSchema().getRandomTableNonEmptyTables();
        gen = new TiDBExpressionGenerator(state).setColumns(tables.getColumns());
        select = new TiDBSelect();

        List<TiDBExpression> fetchColumns = new ArrayList<>();
        fetchColumns.addAll(Randomly.nonEmptySubset(tables.getColumns()).stream().map(c -> new TiDBColumnReference(c))
                .collect(Collectors.toList()));
        select.setFetchColumns(fetchColumns);

        List<TiDBExpression> tableList = tables.getTables().stream().map(t -> new TiDBTableReference(t))
                .collect(Collectors.toList());
        List<TiDBExpression> joins = TiDBJoin.getJoinsWithoutNature(tableList, state);
        select.setJoinList(joins);
        select.setFromList(tableList);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(select.getFetchColumns());
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateHavingClause());
            }
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(gen.generateExpression());
        }
        if (Randomly.getBoolean()) {
            select.setOffsetClause(gen.generateExpression());
        }


        String queryString1 = TiDBVisitor.asString(select);
        double rowCount1 = getRow(state, queryString1, queryPlan1Sequences);


        boolean increase = mutate(Mutator.DISTINCT);


        String queryString2 = TiDBVisitor.asString(select);
        double rowCount2 = getRow(state, queryString2, queryPlan2Sequences);


        if (!DBMSCommon.areQueryPlanSequencesSimilar(queryPlan1Sequences, queryPlan2Sequences)) {
            return;
        }

        

        if (increase && rowCount1 > (rowCount2 + 1) || !increase && (rowCount1 + 1) < rowCount2) {
            throw new AssertionError("Inconsistent result for query: EXPLAIN " + queryString1 + "; --" + rowCount1
                    + "\nEXPLAIN " + queryString2 + "; --" + rowCount2);
        }
    }

    @Override
    protected boolean mutateJoin() {
        if (select.getJoinList().isEmpty()) {
            return false;
        }
        TiDBJoin join = (TiDBJoin) Randomly.fromList(select.getJoinList());



        if (join.getJoinType() == JoinType.CROSS) {
            List<TiDBColumn> columns = new ArrayList<>();
            columns.addAll(((TiDBTableReference) join.getLeftTable()).getTable().getColumns());
            columns.addAll(((TiDBTableReference) join.getRightTable()).getTable().getColumns());
            TiDBExpressionGenerator joinGen2 = new TiDBExpressionGenerator(state).setColumns(columns);
            join.setOnCondition(joinGen2.generateExpression());
        }

        JoinType newJoinType = TiDBJoin.JoinType.INNER;
        if (join.getJoinType() == JoinType.LEFT || join.getJoinType() == JoinType.RIGHT) {


            newJoinType = JoinType.getRandomExcept(JoinType.NATURAL, JoinType.LEFT, JoinType.RIGHT);
        } else {
            newJoinType = JoinType.getRandomExcept(JoinType.NATURAL, join.getJoinType());
        }
        assert newJoinType != JoinType.NATURAL;
        boolean increase = join.getJoinType().ordinal() < newJoinType.ordinal();
        join.setJoinType(newJoinType);
        if (newJoinType == JoinType.CROSS) {
            join.setOnCondition(null);
        }
        return increase;
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
            select.clearHavingClause();
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
            TiDBExpression newWhere = new TiDBBinaryLogicalOperation(select.getWhereClause(), gen.generateExpression(),
                    TiDBBinaryLogicalOperator.AND);
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
            TiDBExpression newWhere = new TiDBBinaryLogicalOperation(select.getWhereClause(), gen.generateExpression(),
                    TiDBBinaryLogicalOperator.OR);
            select.setWhereClause(newWhere);
            return true;
        }
    }

    @Override
    protected boolean mutateLimit() {
        boolean increase = select.getLimitClause() != null;
        if (increase) {
            select.setLimitClause(null);
        } else {
            select.setLimitClause(gen.generateConstant(TiDBDataType.INT));
        }
        return increase;
    }

    private double getRow(SQLGlobalState<?, ?> globalState, String selectStr, List<String> queryPlanSequences)
            throws AssertionError, SQLException {
        double row = -1;
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
                    double estRows = Double.parseDouble(rs.getString(2));
                    if (row == -1) {
                        row = estRows;
                    }
                    String operation = rs.getString(1).split("_")[0];
                    queryPlanSequences.add(operation);
                    return estRows;
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
