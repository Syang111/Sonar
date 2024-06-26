package Sonar.mariadb.ast;

import Sonar.Randomly;

public class MariaDBUnaryPrefixOperation implements MariaDBExpression {

    private MariaDBExpression expr;
    private MariaDBUnaryPrefixOperator op;

    public enum MariaDBUnaryPrefixOperator {

        PLUS("+"), MINUS("-");

        String textRepresentation;

        MariaDBUnaryPrefixOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static MariaDBUnaryPrefixOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public MariaDBUnaryPrefixOperation(MariaDBExpression expr, MariaDBUnaryPrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    public MariaDBExpression getExpr() {
        return expr;
    }

    public MariaDBUnaryPrefixOperator getOp() {
        return op;
    }

}
