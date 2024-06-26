package Sonar.tidb.oracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.common.gen.ExpressionGenerator;
import Sonar.common.oracle.TernaryLogicPartitioningOracleBase;
import Sonar.common.oracle.TestOracle;
import Sonar.tidb.TiDBErrors;
import Sonar.tidb.TiDBExpressionGenerator;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.TiDBSchema;
import Sonar.tidb.TiDBSchema.TiDBTable;
import Sonar.tidb.TiDBSchema.TiDBTables;
import Sonar.tidb.ast.TiDBColumnReference;
import Sonar.tidb.ast.TiDBExpression;
import Sonar.tidb.ast.TiDBJoin;
import Sonar.tidb.ast.TiDBSelect;
import Sonar.tidb.ast.TiDBTableReference;
import Sonar.tidb.gen.TiDBHintGenerator;

public abstract class TiDBTLPBase extends TernaryLogicPartitioningOracleBase<TiDBExpression, TiDBGlobalState>
        implements TestOracle<TiDBGlobalState> {

    TiDBSchema s;
    TiDBTables targetTables;
    TiDBExpressionGenerator gen;
    TiDBSelect select;

    public TiDBTLPBase(TiDBGlobalState state) {
        super(state);
        TiDBErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        gen = new TiDBExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new TiDBSelect();
        select.setFetchColumns(generateFetchColumns());
        List<TiDBTable> tables = targetTables.getTables();
        if (Randomly.getBoolean()) {
            TiDBHintGenerator.generateHints(select, tables);
        }

        List<TiDBExpression> tableList = tables.stream().map(t -> new TiDBTableReference(t))
                .collect(Collectors.toList());
        List<TiDBExpression> joins = TiDBJoin.getJoins(tableList, state);
        select.setJoinList(joins);
        select.setFromList(tableList);
        select.setWhereClause(null);
    }

    List<TiDBExpression> generateFetchColumns() {
        return Arrays.asList(new TiDBColumnReference(targetTables.getColumns().get(0)));
    }

    @Override
    protected ExpressionGenerator<TiDBExpression> getGen() {
        return gen;
    }

}
