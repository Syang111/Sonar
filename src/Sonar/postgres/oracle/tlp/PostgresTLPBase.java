package Sonar.postgres.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.common.gen.ExpressionGenerator;
import Sonar.common.oracle.TernaryLogicPartitioningOracleBase;
import Sonar.common.oracle.TestOracle;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema;
import Sonar.postgres.PostgresSchema.PostgresColumn;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.PostgresSchema.PostgresTable;
import Sonar.postgres.PostgresSchema.PostgresTables;
import Sonar.postgres.ast.PostgresColumnValue;
import Sonar.postgres.ast.PostgresConstant;
import Sonar.postgres.ast.PostgresExpression;
import Sonar.postgres.ast.PostgresJoin;
import Sonar.postgres.ast.PostgresSelect;
import Sonar.postgres.ast.PostgresSelect.ForClause;
import Sonar.postgres.ast.PostgresSelect.PostgresFromTable;
import Sonar.postgres.ast.PostgresSelect.PostgresSubquery;
import Sonar.postgres.gen.PostgresCommon;
import Sonar.postgres.gen.PostgresExpressionGenerator;
import Sonar.postgres.oracle.PostgresNoRECOracle;

public class PostgresTLPBase extends TernaryLogicPartitioningOracleBase<PostgresExpression, PostgresGlobalState>
        implements TestOracle<PostgresGlobalState> {

    protected PostgresSchema s;
    protected PostgresTables targetTables;
    protected PostgresExpressionGenerator gen;
    protected PostgresSelect select;

    public PostgresTLPBase(PostgresGlobalState state) {
        super(state);
        PostgresCommon.addCommonExpressionErrors(errors);
        PostgresCommon.addCommonFetchErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        List<PostgresTable> tables = targetTables.getTables();
        List<PostgresJoin> joins = getJoinStatements(state, targetTables.getColumns(), tables);
        generateSelectBase(tables, joins);
    }

    protected List<PostgresJoin> getJoinStatements(PostgresGlobalState globalState, List<PostgresColumn> columns,
            List<PostgresTable> tables) {
        return PostgresNoRECOracle.getJoinStatements(state, columns, tables);

    }

    protected void generateSelectBase(List<PostgresTable> tables, List<PostgresJoin> joins) {
        List<PostgresExpression> tableList = tables.stream().map(t -> new PostgresFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList());
        gen = new PostgresExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new PostgresSelect();
        select.setFetchColumns(generateFetchColumns());
        select.setFromList(tableList);
        select.setWhereClause(null);
        select.setJoinClauses(joins);
        if (Randomly.getBoolean()) {
            select.setForClause(ForClause.getRandom());
        }
    }

    List<PostgresExpression> generateFetchColumns() {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return Arrays.asList(new PostgresColumnValue(PostgresColumn.createDummy("*"), null));
        }
        List<PostgresExpression> fetchColumns = new ArrayList<>();
        List<PostgresColumn> targetColumns = Randomly.nonEmptySubset(targetTables.getColumns());
        for (PostgresColumn c : targetColumns) {
            fetchColumns.add(new PostgresColumnValue(c, null));
        }
        return fetchColumns;
    }

    @Override
    protected ExpressionGenerator<PostgresExpression> getGen() {
        return gen;
    }

    public static PostgresSubquery createSubquery(PostgresGlobalState globalState, String name, PostgresTables tables) {
        List<PostgresExpression> columns = new ArrayList<>();
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(globalState).setColumns(tables.getColumns());
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            columns.add(gen.generateExpression(0));
        }
        PostgresSelect select = new PostgresSelect();
        select.setFromList(tables.getTables().stream().map(t -> new PostgresFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList()));
        select.setFetchColumns(columns);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(PostgresConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            if (Randomly.getBoolean()) {
                select.setOffsetClause(
                        PostgresConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            }
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setForClause(ForClause.getRandom());
        }
        return new PostgresSubquery(select, name);
    }

}
