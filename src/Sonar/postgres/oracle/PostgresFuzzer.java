package Sonar.postgres.oracle;

import Sonar.Randomly;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresVisitor;
import Sonar.postgres.gen.PostgresRandomQueryGenerator;

public class PostgresFuzzer implements TestOracle<PostgresGlobalState> {

    private final PostgresGlobalState globalState;

    public PostgresFuzzer(PostgresGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public void check() throws Exception {
        String s = PostgresVisitor.asString(
                PostgresRandomQueryGenerator.createRandomQuery(Randomly.smallNumber() + 1, globalState)) + ';';
        try {
            globalState.executeStatement(new SQLQueryAdapter(s));
            globalState.getManager().incrementSelectQueryCount();
        } catch (Error e) {

        }
    }

}
