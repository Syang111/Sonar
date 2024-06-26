package Sonar.tidb.ast;

import Sonar.Randomly;
import Sonar.common.ast.BinaryOperatorNode;
import Sonar.common.ast.BinaryOperatorNode.Operator;
import Sonar.tidb.ast.TiDBBinaryArithmeticOperation.TiDBBinaryArithmeticOperator;

public class TiDBBinaryArithmeticOperation extends BinaryOperatorNode<TiDBExpression, TiDBBinaryArithmeticOperator>
        implements TiDBExpression {

    public enum TiDBBinaryArithmeticOperator implements Operator {
        ADD("+"),
        MINUS("-"),
        MULT("*"),
        DIV("/"),
        INTEGER_DIV("DIV"),
        MOD("%"),
        AND("&"),
        OR("|"),
        XOR("^"),
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT(">>");

        String textRepresentation;

        TiDBBinaryArithmeticOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static TiDBBinaryArithmeticOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }
    }

    public TiDBBinaryArithmeticOperation(TiDBExpression left, TiDBExpression right, TiDBBinaryArithmeticOperator op) {
        super(left, right, op);
    }

}
