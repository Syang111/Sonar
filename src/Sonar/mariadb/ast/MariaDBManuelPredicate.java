package Sonar.mariadb.ast;

public class MariaDBManuelPredicate implements MariaDBExpression{

    private final String predicate;

    public MariaDBManuelPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getString() {
        return predicate;
    }

}
