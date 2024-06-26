package Sonar.tidb.visitor;

import Sonar.tidb.ast.*;

public interface TiDBVisitor {

    default void visit(TiDBExpression expr) {
        if (expr instanceof TiDBConstant) {
            visit((TiDBConstant) expr);
        } else if (expr instanceof TiDBManuelPredicate) {
            visit((TiDBManuelPredicate) expr);
        } else if (expr instanceof TiDBPostfixText) {
            visit((TiDBPostfixText) expr);
        } else if (expr instanceof TiDBColumnReference) {
            visit((TiDBColumnReference) expr);
        } else if (expr instanceof TiDBSelect) {
            visit((TiDBSelect) expr);
        } else if (expr instanceof TiDBTableReference) {
            visit((TiDBTableReference) expr);
        } else if (expr instanceof TiDBFunctionCall) {
            visit((TiDBFunctionCall) expr);
        } else if (expr instanceof TiDBJoin) {
            visit((TiDBJoin) expr);
        } else if (expr instanceof TiDBText) {
            visit((TiDBText) expr);
        } else if (expr instanceof TiDBAggregate) {
            visit((TiDBAggregate) expr);
        } else if (expr instanceof TiDBCastOperation) {
            visit((TiDBCastOperation) expr);
        } else if (expr instanceof TiDBCase) {
            visit((TiDBCase) expr);
        } else if (expr instanceof TiDBDateExpression) {
            visit((TiDBDateExpression) expr);
        }else if (expr instanceof TiDBBinaryArithmeticOperation) {
            visit((TiDBBinaryArithmeticOperation) expr);
        }else if (expr instanceof TiDBBinaryComparisonOperation) {
            visit((TiDBBinaryComparisonOperation) expr);
        }else if (expr instanceof TiDBBinaryBitOperation) {
            visit((TiDBBinaryBitOperation) expr);
        }else if (expr instanceof TiDBBinaryLogicalOperation) {
            visit((TiDBBinaryLogicalOperation) expr);
        }else if (expr instanceof TiDBRegexOperation) {
            visit((TiDBRegexOperation) expr);
        }else if (expr instanceof TiDBUnaryPostfixOperation) {
            visit((TiDBUnaryPostfixOperation) expr);
        } else if (expr instanceof TiDBUnaryPrefixOperation) {
            visit((TiDBUnaryPrefixOperation) expr);
        } else {
            throw new AssertionError(expr.getClass());
        }
    }

    void visit(TiDBManuelPredicate predicate);

    void visit(TiDBBinaryArithmeticOperation op);

    void visit(TiDBBinaryComparisonOperation op);

    void visit(TiDBBinaryBitOperation op);

    void visit(TiDBBinaryLogicalOperation op);

    void visit(TiDBRegexOperation op);

    void visit(TiDBUnaryPostfixOperation op);

    void visit(TiDBUnaryPrefixOperation op);

    void visit(TiDBDateExpression op);

    void visit(TiDBCase caseExpr);

    void visit(TiDBCastOperation cast);

    void visit(TiDBAggregate aggr);

    void visit(TiDBFunctionCall call);

    void visit(TiDBConstant expr);

    void visit(TiDBColumnReference expr);

    void visit(TiDBTableReference expr);

    void visit(TiDBSelect select);

    void visit(TiDBJoin join);

    void visit(TiDBText text);

    void visit(TiDBPostfixText text);

    static String asString(TiDBExpression expr) {
        TiDBToStringVisitor v = new TiDBToStringVisitor();
        v.visit(expr);
        return v.getString();
    }

}
