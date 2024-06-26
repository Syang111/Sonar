package Sonar.mysql.ast;


public class MySQLManuelPredicate implements MySQLExpression {

    private final String predicate;

    public MySQLManuelPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getString() {
        return predicate;
    }

}
