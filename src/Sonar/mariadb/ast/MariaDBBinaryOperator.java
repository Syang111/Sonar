package Sonar.mariadb.ast;

import Sonar.Randomly;

public class MariaDBBinaryOperator implements MariaDBExpression {

    private MariaDBExpression left;
    private MariaDBExpression right;
    private MariaDBBinaryComparisonOperator op;

    public enum MariaDBBinaryComparisonOperator {
        NOT_EQUAL("!="), LESS_THAN("<"),  GREATER_THAN(">"),
        GREATER_THAN_EQUAL(">="),


        LIKE("LIKE"),

         PLUS("+"), MINUS("-"), MULTIPLY("*"), DIVIDE("/"), MOD("%"),
        AND("AND"), OR("OR"), XOR("XOR"),

        BITWISE_AND("&"), LEFT_SHIFT("<<"), RIGHT_SHIFT(">>"), BITWISE_XOR("^"), BITWISE_OR("|");

        private String op;

        MariaDBBinaryComparisonOperator(String op) {
            this.op = op;
        }

        public String getTextRepresentation() {
            return op;
        }

        public static MariaDBBinaryComparisonOperator getRandom() {
            return Randomly.fromOptions(MariaDBBinaryComparisonOperator.values());
        }

    };

    public MariaDBBinaryOperator(MariaDBExpression left, MariaDBExpression right, MariaDBBinaryComparisonOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public MariaDBExpression getLeft() {
        return left;
    }

    public MariaDBExpression getRight() {
        return right;
    };

    public MariaDBBinaryComparisonOperator getOp() {
        return op;
    }

}
