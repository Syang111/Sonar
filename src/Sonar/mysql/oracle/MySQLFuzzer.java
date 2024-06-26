package Sonar.mysql.oracle;

import Sonar.Randomly;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLVisitor;
import Sonar.mysql.gen.MySQLRandomQuerySynthesizer;

public class MySQLFuzzer implements TestOracle<MySQLGlobalState> {

    private final MySQLGlobalState globalState;

    public MySQLFuzzer(MySQLGlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public void check() throws Exception {
        String s = MySQLVisitor.asString(MySQLRandomQuerySynthesizer.generate(globalState, Randomly.smallNumber() + 1))
                + ';';
        try {
            globalState.executeStatement(new SQLQueryAdapter(s));
            globalState.getManager().incrementSelectQueryCount();
        } catch (Error e) {

        }
    }

}
