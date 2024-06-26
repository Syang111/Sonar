package Sonar.postgres.gen;

import java.util.List;
import java.util.stream.Collectors;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema.PostgresIndex;

public final class PostgresReindexGenerator {

    private PostgresReindexGenerator() {
    }

    private enum Scope {
        INDEX, TABLE, DATABASE;
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("could not create unique index");
        StringBuilder sb = new StringBuilder();
        sb.append("REINDEX");



        sb.append(" ");
        Scope scope = Randomly.fromOptions(Scope.values());
        switch (scope) {
        case INDEX:
            sb.append("INDEX ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            List<PostgresIndex> indexes = globalState.getSchema().getRandomTable().getIndexes();
            if (indexes.isEmpty()) {
                throw new IgnoreMeException();
            }
            sb.append(indexes.stream().map(i -> i.getIndexName()).collect(Collectors.joining()));
            break;
        case TABLE:
            sb.append("TABLE ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            sb.append(globalState.getSchema().getRandomTable(t -> !t.isView()).getName());
            break;
        case DATABASE:
            sb.append("DATABASE ");
            if (Randomly.getBoolean()) {
                sb.append("CONCURRENTLY ");
            }
            sb.append(globalState.getSchema().getDatabaseName());
            break;
        default:
            throw new AssertionError(scope);
        }
        errors.add("already contains data");
        errors.add("does not exist");
        errors.add("REINDEX is not yet implemented for partitioned indexes");
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
