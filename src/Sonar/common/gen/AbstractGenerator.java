package Sonar.common.gen;

import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;

public abstract class AbstractGenerator {

    protected final ExpectedErrors errors = new ExpectedErrors();
    protected final StringBuilder sb = new StringBuilder();
    protected boolean canAffectSchema;

    public SQLQueryAdapter getQuery() {
        buildStatement();
        return new SQLQueryAdapter(sb.toString(), errors, canAffectSchema);
    }

    public abstract void buildStatement();

}
