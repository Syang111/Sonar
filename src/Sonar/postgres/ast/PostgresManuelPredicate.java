package Sonar.postgres.ast;

public class PostgresManuelPredicate implements PostgresExpression {
    private final String predicate;

    public PostgresManuelPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getString() {
        return predicate;
    }
}
