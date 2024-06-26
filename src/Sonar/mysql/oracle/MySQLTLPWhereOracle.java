package Sonar.mysql.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Sonar.ComparatorHelper;
import Sonar.Randomly;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLVisitor;

public class MySQLTLPWhereOracle extends MySQLQueryPartitioningBase {

    public MySQLTLPWhereOracle(MySQLGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setWhereClause(null);
        String originalQueryString = MySQLVisitor.asString(select);


        errors.add("BIGINT value is out of range");
        errors.add("is not valid for CHARACTER SET");
        errors.add("Data truncation");
        errors.add("Incorrect DATETIME value");
        errors.add("Incorrect DATE value");
        errors.add("Invalid use of group function");
        errors.add("incompatible with sql_mode=only_full_group_by");
        errors.add("Cannot convert string");

        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        if (Randomly.getBoolean()) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        select.setOrderByExpressions(Collections.emptyList());
        select.setWhereClause(predicate);
        String firstQueryString = MySQLVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MySQLVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MySQLVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, Randomly.getBoolean(), state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

}
