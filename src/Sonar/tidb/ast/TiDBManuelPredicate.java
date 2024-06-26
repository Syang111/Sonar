package Sonar.tidb.ast;

public class TiDBManuelPredicate implements TiDBExpression {

    private final String predicate;

    public TiDBManuelPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getString() {
        return predicate;
    }


}
