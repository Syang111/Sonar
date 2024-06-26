package Sonar.mysql.ast;

import java.util.List;

import Sonar.IgnoreMeException;


public class MySQLInOperation implements MySQLExpression {

    private final MySQLExpression expr;
    private final List<MySQLExpression> listElements;
    private final boolean isTrue;

    public MySQLInOperation(MySQLExpression expr, List<MySQLExpression> listElements, boolean isTrue) {
        this.expr = expr;
        this.listElements = listElements;
        this.isTrue = isTrue;
    }

    public MySQLExpression getExpr() {
        return expr;
    }

    public List<MySQLExpression> getListElements() {
        return listElements;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant leftVal = expr.getExpectedValue();
        if (leftVal.isNull()) {
            return MySQLConstant.createNullConstant();
        }

        if (leftVal.isInt() && !leftVal.isSigned()) {
            throw new IgnoreMeException();
        }

        boolean isNull = false;
        for (MySQLExpression rightExpr : listElements) {
            MySQLConstant rightVal = rightExpr.getExpectedValue();


            if (rightVal.isInt() && !rightVal.isSigned()) {
                throw new IgnoreMeException();
            }
            MySQLConstant convertedRightVal = rightVal;
            MySQLConstant isEquals = leftVal.isEquals(convertedRightVal);
            if (isEquals.isNull()) {
                isNull = true;
            } else {
                if (isEquals.getInt() == 1) {
                    return MySQLConstant.createBoolean(isTrue);
                }
            }
        }
        if (isNull) {
            return MySQLConstant.createNullConstant();
        } else {
            return MySQLConstant.createBoolean(!isTrue);
        }

    }

    public boolean isTrue() {
        return isTrue;
    }
}
