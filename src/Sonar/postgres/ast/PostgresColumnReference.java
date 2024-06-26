package Sonar.postgres.ast;

import Sonar.postgres.PostgresSchema.PostgresColumn;

public class PostgresColumnReference implements PostgresExpression {
    private final PostgresColumn c;

    public PostgresColumnReference(PostgresColumn c) {
        this.c = c;
    }

    public PostgresColumn getColumn() {
        return c;
    }
}
