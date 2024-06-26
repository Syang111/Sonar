package Sonar.sqlite3.oracle;

import Sonar.Randomly;
import Sonar.common.oracle.TestOracle;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.sqlite3.SQLite3GlobalState;
import Sonar.sqlite3.SQLite3Visitor;


public class SQLite3Fuzzer implements TestOracle<SQLite3GlobalState> {

    private final SQLite3GlobalState globalState;

    public SQLite3Fuzzer(SQLite3GlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public void check() throws Exception {
        String s = SQLite3Visitor
                .asString(SQLite3RandomQuerySynthesizer.generate(globalState, Randomly.smallNumber() + 1)) + ";";
        try {
            globalState.executeStatement(new SQLQueryAdapter(s));
            globalState.getManager().incrementSelectQueryCount();
        } catch (Error e) {

        }
    }

}
