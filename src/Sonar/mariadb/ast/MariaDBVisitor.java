package Sonar.mariadb.ast;

import Sonar.mysql.ast.MySQLDataExpression;

public abstract class MariaDBVisitor {

    public abstract void visit(MariaDBPostfixText text);
    public abstract void visit(MariaDBConstant c);
    public abstract void visit(MariaDBDateExpression date);
    public abstract void visit(MariaDBPostfixUnaryOperation op);

    public abstract void visit(MariaDBColumnName c);

    public abstract void visit(MariaDBSelectStatement s);

    public abstract void visit(MariaDBText t);

    public abstract void visit(MariaDBAggregate aggr);

    public abstract void visit(MariaDBBinaryOperator comp);

    public abstract void visit(MariaDBUnaryPrefixOperation op);

    public abstract void visit(MariaDBFunction func);

    public abstract void visit(MariaDBInOperation op);

    public abstract void visit(MariaDBManuelPredicate op);

    public abstract void visit(MariaDBJoin op);



    public void visit(MariaDBExpression expr) {
        if (expr instanceof MariaDBConstant) {
            visit((MariaDBConstant) expr);
        } else if (expr instanceof MariaDBJoin) {
            visit((MariaDBJoin) expr);
        } else if (expr instanceof MariaDBDateExpression) {
            visit((MariaDBDateExpression) expr);
        } else if (expr instanceof MariaDBManuelPredicate) {
            visit((MariaDBManuelPredicate) expr);
        } else if (expr instanceof MariaDBPostfixText) {
            visit((MariaDBPostfixText) expr);
        } else if (expr instanceof MariaDBColumnName) {
            visit((MariaDBColumnName) expr);
        } else if (expr instanceof MariaDBSelectStatement) {
            visit((MariaDBSelectStatement) expr);
        } else if (expr instanceof MariaDBPostfixUnaryOperation) {
            visit((MariaDBPostfixUnaryOperation) expr);
        } else if (expr instanceof MariaDBText) {
            visit((MariaDBText) expr);
        } else if (expr instanceof MariaDBAggregate) {
            visit((MariaDBAggregate) expr);
        } else if (expr instanceof MariaDBBinaryOperator) {
            visit((MariaDBBinaryOperator) expr);
        } else if (expr instanceof MariaDBUnaryPrefixOperation) {
            visit((MariaDBUnaryPrefixOperation) expr);
        } else if (expr instanceof MariaDBFunction) {
            visit((MariaDBFunction) expr);
        } else if (expr instanceof MariaDBInOperation) {
            visit((MariaDBInOperation) expr);
        } else {
            throw new AssertionError(expr.getClass());
        }
    }

    public static String asString(MariaDBExpression expr) {
        MariaDBStringVisitor v = new MariaDBStringVisitor();
        v.visit(expr);
        return v.getString();
    }

}
