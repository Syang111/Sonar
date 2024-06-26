package Sonar.postgres.gen;

import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;

public final class PostgresTruncateGenerator {

    private PostgresTruncateGenerator() {
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("TRUNCATE");
        if (Randomly.getBoolean()) {
            sb.append(" TABLE");
        }




        sb.append(" ");
        sb.append(globalState.getSchema().getDatabaseTablesRandomSubsetNotEmpty().stream().map(t -> t.getName())
                .collect(Collectors.joining(", ")));
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("RESTART IDENTITY", "CONTINUE IDENTITY"));
        }
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("CASCADE", "RESTRICT"));
        }
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors
                .from("cannot truncate a table referenced in a foreign key constraint", "is not a table"));
    }

}
