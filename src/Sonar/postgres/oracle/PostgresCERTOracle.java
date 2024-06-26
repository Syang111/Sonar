package Sonar.postgres.oracle;

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
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema.PostgresColumn;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.PostgresSchema.PostgresTables;
import Sonar.postgres.PostgresVisitor;
import Sonar.postgres.ast.PostgresBinaryLogicalOperation;
import Sonar.postgres.ast.PostgresBinaryLogicalOperation.BinaryLogicalOperator;
import Sonar.postgres.ast.PostgresColumnReference;
import Sonar.postgres.ast.PostgresConstant;
import Sonar.postgres.ast.PostgresExpression;
import Sonar.postgres.ast.PostgresJoin;
import Sonar.postgres.ast.PostgresJoin.PostgresJoinType;
import Sonar.postgres.ast.PostgresSelect;
import Sonar.postgres.ast.PostgresTableReference;
import Sonar.postgres.gen.PostgresCommon;
import Sonar.postgres.gen.PostgresExpressionGenerator;

public class PostgresCERTOracle extends CERTOracleBase<PostgresGlobalState> implements TestOracle<PostgresGlobalState> {
    private PostgresExpressionGenerator gen;
    private PostgresSelect select;

    public PostgresCERTOracle(PostgresGlobalState globalState) {
        super(globalState);
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonInsertUpdateErrors(errors);
        PostgresCommon.addGroupingErrors(errors);
        PostgresCommon.addCommonInsertUpdateErrors(errors);
        PostgresCommon.addCommonRangeExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        queryPlan1Sequences = new ArrayList<>();
        queryPlan2Sequences = new ArrayList<>();


        PostgresTables tables = state.getSchema().getRandomTableNonEmptyTables();
        List<PostgresExpression> tableList = tables.getTables().stream().map(t -> new PostgresTableReference(t))
                .collect(Collectors.toList());
        gen = new PostgresExpressionGenerator(state).setColumns(tables.getColumns());
        List<PostgresExpression> fetchColumns = new ArrayList<>();
        fetchColumns.addAll(Randomly.nonEmptySubset(tables.getColumns()).stream()
                .map(c -> new PostgresColumnReference(c)).collect(Collectors.toList()));

        select = new PostgresSelect();
        select.setFetchColumns(fetchColumns);
        select.setFromList(tableList);
        List<PostgresExpression> joins = PostgresJoin.getJoins(tableList, state);
        select.setJoinList(joins);

        select.setSelectType(Randomly.fromOptions(PostgresSelect.SelectType.values()));
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(fetchColumns);
            if (Randomly.getBoolean()) {
                select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
            }
        }


        String queryString1 = PostgresVisitor.asString(select);
        int rowCount1 = getRow(state, queryString1, queryPlan1Sequences);


        boolean increase = mutate(Mutator.LIMIT);


        String queryString2 = PostgresVisitor.asString(select);
        int rowCount2 = getRow(state, queryString2, queryPlan2Sequences);


        if (DBMSCommon.editDistance(queryPlan1Sequences, queryPlan2Sequences) > 1) {
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
        PostgresJoin join = (PostgresJoin) Randomly.fromList(select.getJoinList());


        if (join.getType() == PostgresJoinType.CROSS) {
            List<PostgresColumn> columns = new ArrayList<>();
            columns.addAll(((PostgresTableReference) join.getLeftTable()).getTable().getColumns());
            columns.addAll(((PostgresTableReference) join.getRightTable()).getTable().getColumns());
            PostgresExpressionGenerator joinGen2 = new PostgresExpressionGenerator(state).setColumns(columns);
            join.setOnClause(joinGen2.generateExpression(0, PostgresDataType.BOOLEAN));
        }

        PostgresJoinType newJoinType = PostgresJoinType.INNER;
        if (join.getType() == PostgresJoinType.LEFT || join.getType() == PostgresJoinType.RIGHT) {
            newJoinType = PostgresJoinType.getRandomExcept(PostgresJoinType.LEFT, PostgresJoinType.RIGHT);
        } else {
            newJoinType = PostgresJoinType.getRandomExcept(join.getType());
        }
        boolean increase = join.getType().ordinal() < newJoinType.ordinal();
        join.setType(newJoinType);
        if (newJoinType == PostgresJoinType.CROSS) {
            join.setOnClause(null);
        }
        return increase;
    }

    @Override
    protected boolean mutateDistinct() {
        PostgresSelect.SelectType selectType = select.getSelectOption();
        if (selectType != PostgresSelect.SelectType.ALL) {
            select.setSelectType(PostgresSelect.SelectType.ALL);
            return true;
        } else {
            select.setSelectType(PostgresSelect.SelectType.DISTINCT);
            return false;
        }
    }

    @Override
    protected boolean mutateWhere() {
        boolean increase = select.getWhereClause() != null;
        if (increase) {
            select.setWhereClause(null);
        } else {
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
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
            select.setHavingClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
            return false;
        } else {
            if (select.getHavingClause() == null) {
                select.setHavingClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
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
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
        } else {
            PostgresExpression newWhere = new PostgresBinaryLogicalOperation(select.getWhereClause(),
                    gen.generateExpression(0, PostgresDataType.BOOLEAN), BinaryLogicalOperator.AND);
            select.setWhereClause(newWhere);
        }
        return false;
    }

    @Override
    protected boolean mutateOr() {
        if (select.getWhereClause() == null) {
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
            return false;
        } else {
            PostgresExpression newWhere = new PostgresBinaryLogicalOperation(select.getWhereClause(),
                    gen.generateExpression(0, PostgresDataType.BOOLEAN), BinaryLogicalOperator.OR);
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
            Randomly r = new Randomly();
            select.setLimitClause(PostgresConstant.createIntConstant((int) Math.abs(r.getInteger())));
        }
        return increase;
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
                    String content = rs.getString(1).trim();
                    if (content.contains("rows=")) {
                        try {
                            int ind = content.indexOf("rows=");
                            int number = Integer.parseInt(content.substring(ind + 5).split(" ")[0]);
                            if (row == -1) {
                                row = number;

                            }
                        } catch (Exception e) {
                        }
                    }

                    String[] planPart = content.split("-> ");
                    String plan = planPart[planPart.length - 1];
                    queryPlanSequences.add(plan.split("  ")[0].trim());
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
