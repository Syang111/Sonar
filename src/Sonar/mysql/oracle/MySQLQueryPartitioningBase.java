package Sonar.mysql.oracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.common.gen.ExpressionGenerator;
import Sonar.common.oracle.TernaryLogicPartitioningOracleBase;
import Sonar.common.oracle.TestOracle;
import Sonar.mysql.MySQLErrors;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLSchema;
import Sonar.mysql.MySQLSchema.MySQLTable;
import Sonar.mysql.MySQLSchema.MySQLTables;
import Sonar.mysql.ast.MySQLColumnReference;
import Sonar.mysql.ast.MySQLExpression;
import Sonar.mysql.ast.MySQLSelect;
import Sonar.mysql.ast.MySQLTableReference;
import Sonar.mysql.gen.MySQLExpressionGenerator;

public abstract class MySQLQueryPartitioningBase extends
        TernaryLogicPartitioningOracleBase<MySQLExpression, MySQLGlobalState> implements TestOracle<MySQLGlobalState> {

    MySQLSchema s;
    MySQLTables targetTables;
    MySQLExpressionGenerator gen;
    MySQLSelect select;

    public MySQLQueryPartitioningBase(MySQLGlobalState state) {
        super(state);
        MySQLErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        gen = new MySQLExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new MySQLSelect();
        select.setFetchColumns(generateFetchColumns());
        List<MySQLTable> tables = targetTables.getTables();
        List<MySQLExpression> tableList = tables.stream().map(t -> new MySQLTableReference(t))
                .collect(Collectors.toList());

        select.setFromList(tableList);
        select.setWhereClause(null);

    }

    List<MySQLExpression> generateFetchColumns() {
        return Arrays.asList(MySQLColumnReference.create(targetTables.getColumns().get(0), null));
    }

    List<MySQLExpression> generateFetchColumnExpression() {
        return Arrays.asList(MySQLColumnReference.create(targetTables.getColumns().get(0), null));
    }

    @Override
    protected ExpressionGenerator<MySQLExpression> getGen() {
        return gen;
    }

}
