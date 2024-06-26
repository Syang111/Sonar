package Sonar.tidb.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Sonar.ComparatorHelper;
import Sonar.Randomly;
import Sonar.common.oracle.TestOracle;
import Sonar.tidb.TiDBErrors;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.ast.TiDBExpression;
import Sonar.tidb.visitor.TiDBVisitor;

public class TiDBTLPHavingOracle extends TiDBTLPBase implements TestOracle<TiDBGlobalState> {

    private String generatedQueryString;

    public TiDBTLPHavingOracle(TiDBGlobalState state) {
        super(state);
        TiDBErrors.addExpressionHavingErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        boolean orderBy = Randomly.getBoolean();
        if (orderBy) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        select.setHavingClause(null);
        String originalQueryString = TiDBVisitor.asString(select);
        generatedQueryString = originalQueryString;
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        select.setHavingClause(predicate);
        String firstQueryString = TiDBVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = TiDBVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = TiDBVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !orderBy, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

    @Override
    protected TiDBExpression generatePredicate() {
        return gen.generateHavingClause();
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }
}
