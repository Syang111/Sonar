package Sonar.postgres.gen;

import Sonar.Randomly;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.PostgresSchema.PostgresTable;
import Sonar.postgres.PostgresVisitor;

public final class PostgresDeleteGenerator {

    private PostgresDeleteGenerator() {
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        PostgresTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("violates foreign key constraint");
        errors.add("violates not-null constraint");
        errors.add("could not determine which collation to use for string comparison");
        StringBuilder sb = new StringBuilder("DELETE FROM");
        if (Randomly.getBoolean()) {
            sb.append(" ONLY");
        }
        sb.append(" ");
        sb.append(table.getName());
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            sb.append(PostgresVisitor.asString(PostgresExpressionGenerator.generateExpression(globalState,
                    table.getColumns(), PostgresDataType.BOOLEAN)));
        }
        if (Randomly.getBoolean()) {
            sb.append(" RETURNING ");
            sb.append(PostgresVisitor
                    .asString(PostgresExpressionGenerator.generateExpression(globalState, table.getColumns())));
        }
        PostgresCommon.addCommonExpressionErrors(errors);
        errors.add("out of range");
        errors.add("cannot cast");
        errors.add("invalid input syntax for");
        errors.add("division by zero");
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
