package Sonar.postgres.ast;

import Sonar.postgres.PostgresSchema.PostgresTable;

public class PostgresTableReference implements PostgresExpression {
    private final PostgresTable table;

    public PostgresTableReference(PostgresTable table) {
        this.table = table;
    }

    public PostgresTable getTable() {
        return table;
    }
}
