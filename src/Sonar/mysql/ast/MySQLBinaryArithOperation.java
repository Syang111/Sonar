package Sonar.mysql.ast;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.sqlite3.SQLite3Provider;
import Sonar.sqlite3.ast.SQLite3Cast;
import Sonar.sqlite3.ast.SQLite3Constant;
import Sonar.sqlite3.schema.SQLite3DataType;

import java.util.function.BinaryOperator;

public class MySQLBinaryArithOperation implements MySQLExpression {
    private final MySQLExpression left;
    private final MySQLExpression right;
    private final MySQLBinaryArithOperator op;

    public MySQLBinaryArithOperation(MySQLExpression left, MySQLExpression right, MySQLBinaryArithOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public enum MySQLBinaryArithOperator {
        MULTIPLY("*") {

        },



        REMAINDER("%") {
        },

        MOD("MOD") {
        },

        PLUS("+") {
        },

        MINUS("-") {
        },

        DIV("DIV") {
        },
        SHIFT_LEFT("<<") {

        },
        SHIFT_RIGHT(">>") {

        };

        private String textRepresentation;

        MySQLBinaryArithOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public String getTextRepresentation() {
            return textRepresentation;
        }

        public static MySQLBinaryArithOperation.MySQLBinaryArithOperator getRandom() {
            return Randomly.fromOptions(values());
        }
    }


    public MySQLExpression getLeft() {
        return left;
    }

    public MySQLBinaryArithOperator getOp() {
        return op;
    }

    public MySQLExpression getRight() {
        return right;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return null;
    }
}
