package Sonar.postgres.gen;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema.PostgresTable;

public final class PostgresClusterGenerator {

    private PostgresClusterGenerator() {
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("there is no previously clustered index for table");
        errors.add("cannot cluster a partitioned table");
        errors.add("access method does not support clustering");
        StringBuilder sb = new StringBuilder("CLUSTER ");
        if (Randomly.getBoolean()) {
            PostgresTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
            sb.append(table.getName());
            if (Randomly.getBoolean() && !table.getIndexes().isEmpty()) {
                sb.append(" USING ");
                sb.append(table.getRandomIndex().getIndexName());
                errors.add("cannot cluster on partial index");
            }
        }
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
