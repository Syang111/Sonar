package Sonar.postgres.gen;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;

public final class PostgresSequenceGenerator {

    private PostgresSequenceGenerator() {
    }

    public static SQLQueryAdapter createSequence(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder("CREATE");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("TEMPORARY", "TEMP"));
        }
        sb.append(" SEQUENCE");

        sb.append(" IF NOT EXISTS");

        sb.append(" seq");
        if (Randomly.getBoolean()) {
            sb.append(" AS ");
            sb.append(Randomly.fromOptions("smallint", "integer", "bigint"));
        }
        if (Randomly.getBoolean()) {
            sb.append(" INCREMENT");
            if (Randomly.getBoolean()) {
                sb.append(" BY");
            }
            sb.append(" ");
            sb.append(globalState.getRandomly().getInteger());
            errors.add("INCREMENT must not be zero");
        }
        if (Randomly.getBoolean()) {
            if (Randomly.getBoolean()) {
                sb.append(" MINVALUE");
                sb.append(" ");
                sb.append(globalState.getRandomly().getInteger());
            } else {
                sb.append(" NO MINVALUE");
            }
            errors.add("must be less than MAXVALUE");
        }
        if (Randomly.getBoolean()) {
            if (Randomly.getBoolean()) {
                sb.append(" MAXVALUE");
                sb.append(" ");
                sb.append(globalState.getRandomly().getInteger());
            } else {
                sb.append(" NO MAXVALUE");
            }
            errors.add("must be less than MAXVALUE");
        }
        if (Randomly.getBoolean()) {
            sb.append(" START");
            if (Randomly.getBoolean()) {
                sb.append(" WITH");
            }
            sb.append(" ");
            sb.append(globalState.getRandomly().getInteger());
            errors.add("cannot be less than MINVALUE");
            errors.add("cannot be greater than MAXVALUE");
        }
        if (Randomly.getBoolean()) {
            sb.append(" CACHE ");
            sb.append(globalState.getRandomly().getPositiveIntegerNotNull());
        }
        errors.add("is out of range");
        if (Randomly.getBoolean()) {
            if (Randomly.getBoolean()) {
                sb.append(" NO");
            }
            sb.append(" CYCLE");
        }
        if (Randomly.getBoolean()) {
            sb.append(" OWNED BY ");

            sb.append("NONE");



        }
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
