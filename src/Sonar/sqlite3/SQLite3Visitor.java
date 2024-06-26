package Sonar.sqlite3;

import Sonar.sqlite3.ast.SQLite3Aggregate;
import Sonar.sqlite3.ast.SQLite3Case.SQLite3CaseWithBaseExpression;
import Sonar.sqlite3.ast.SQLite3Case.SQLite3CaseWithoutBaseExpression;
import Sonar.sqlite3.ast.SQLite3Constant;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Expression.BetweenOperation;
import Sonar.sqlite3.ast.SQLite3Expression.BinaryComparisonOperation;
import Sonar.sqlite3.ast.SQLite3Expression.Cast;
import Sonar.sqlite3.ast.SQLite3Expression.CollateOperation;
import Sonar.sqlite3.ast.SQLite3Expression.Function;
import Sonar.sqlite3.ast.SQLite3Expression.InOperation;
import Sonar.sqlite3.ast.SQLite3Expression.Join;
import Sonar.sqlite3.ast.SQLite3Expression.MatchOperation;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3Distinct;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3Exist;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3OrderingTerm;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixText;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3PostfixUnaryOperation;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3TableReference;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3ManuelPredicate;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3Text;
import Sonar.sqlite3.ast.SQLite3Expression.Sqlite3BinaryOperation;
import Sonar.sqlite3.ast.SQLite3Expression.Subquery;
import Sonar.sqlite3.ast.SQLite3Expression.TypeLiteral;
import Sonar.sqlite3.ast.SQLite3Function;
import Sonar.sqlite3.ast.SQLite3RowValueExpression;
import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.ast.SQLite3SetClause;
import Sonar.sqlite3.ast.SQLite3UnaryOperation;
import Sonar.sqlite3.ast.SQLite3WindowFunction;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression.SQLite3WindowFunctionFrameSpecBetween;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression.SQLite3WindowFunctionFrameSpecTerm;

public interface SQLite3Visitor {

    static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }



    default void visit(BinaryComparisonOperation op) {

    }

    default void visit(Sqlite3BinaryOperation op) {

    }

    default void visit(SQLite3UnaryOperation exp) {

    }

    default void visit(SQLite3PostfixText op) {

    }

    default void visit(SQLite3PostfixUnaryOperation exp) {

    }

    void visit(BetweenOperation op);

    void visit(SQLite3ColumnName c);

    void visit(SQLite3Constant c);

    void visit(Function f);

    void visit(SQLite3Select s, boolean inner);

    void visit(SQLite3OrderingTerm term);

    void visit(SQLite3TableReference tableReference);

    void visit(SQLite3SetClause set);

    void visit(CollateOperation op);

    void visit(Cast cast);

    void visit(TypeLiteral literal);

    void visit(InOperation op);

    void visit(Subquery query);

    void visit(SQLite3Exist exist);

    void visit(Join join);

    void visit(MatchOperation match);

    void visit(SQLite3Function func);

    void visit(SQLite3Text func);

    void visit(SQLite3Distinct distinct);

    void visit(SQLite3CaseWithoutBaseExpression casExpr);

    void visit(SQLite3CaseWithBaseExpression casExpr);

    void visit(SQLite3Aggregate aggr);

    void visit(SQLite3WindowFunction func);

    void visit(SQLite3RowValueExpression rw);

    void visit(SQLite3WindowFunctionExpression windowFunction);

    void visit(SQLite3WindowFunctionFrameSpecTerm term);

    void visit(SQLite3WindowFunctionFrameSpecBetween between);


    void visit(SQLite3ManuelPredicate predicate);

    default void visit(SQLite3Expression expr) {
        if (expr instanceof Sqlite3BinaryOperation) {
            visit((Sqlite3BinaryOperation) expr);
        } else if (expr instanceof SQLite3ColumnName) {
            visit((SQLite3ColumnName) expr);
        } else if (expr instanceof SQLite3Constant) {
            visit((SQLite3Constant) expr);
        } else if (expr instanceof SQLite3UnaryOperation) {
            visit((SQLite3UnaryOperation) expr);
        } else if (expr instanceof SQLite3PostfixUnaryOperation) {
            visit((SQLite3PostfixUnaryOperation) expr);
        } else if (expr instanceof Function) {
            visit((Function) expr);
        } else if (expr instanceof BetweenOperation) {
            visit((BetweenOperation) expr);
        } else if (expr instanceof CollateOperation) {
            visit((CollateOperation) expr);
        } else if (expr instanceof SQLite3OrderingTerm) {
            visit((SQLite3OrderingTerm) expr);
        } else if (expr instanceof SQLite3Expression.InOperation) {
            visit((InOperation) expr);
        } else if (expr instanceof Cast) {
            visit((Cast) expr);
        } else if (expr instanceof Subquery) {
            visit((Subquery) expr);
        } else if (expr instanceof Join) {
            visit((Join) expr);
        } else if (expr instanceof SQLite3Select) {
            visit((SQLite3Select) expr, true);
        } else if (expr instanceof SQLite3Exist) {
            visit((SQLite3Exist) expr);
        } else if (expr instanceof BinaryComparisonOperation) {
            visit((BinaryComparisonOperation) expr);
        } else if (expr instanceof SQLite3Function) {
            visit((SQLite3Function) expr);
        } else if (expr instanceof SQLite3Distinct) {
            visit((SQLite3Distinct) expr);
        } else if (expr instanceof SQLite3CaseWithoutBaseExpression) {
            visit((SQLite3CaseWithoutBaseExpression) expr);
        } else if (expr instanceof SQLite3CaseWithBaseExpression) {
            visit((SQLite3CaseWithBaseExpression) expr);
        } else if (expr instanceof SQLite3Aggregate) {
            visit((SQLite3Aggregate) expr);
        } else if (expr instanceof SQLite3PostfixText) {
            visit((SQLite3PostfixText) expr);
        } else if (expr instanceof SQLite3WindowFunction) {
            visit((SQLite3WindowFunction) expr);
        } else if (expr instanceof MatchOperation) {
            visit((MatchOperation) expr);
        } else if (expr instanceof SQLite3RowValueExpression) {
            visit((SQLite3RowValueExpression) expr);
        } else if (expr instanceof SQLite3Text) {
            visit((SQLite3Text) expr);
        } else if (expr instanceof SQLite3WindowFunctionExpression) {
            visit((SQLite3WindowFunctionExpression) expr);
        } else if (expr instanceof SQLite3WindowFunctionFrameSpecTerm) {
            visit((SQLite3WindowFunctionFrameSpecTerm) expr);
        } else if (expr instanceof SQLite3WindowFunctionFrameSpecBetween) {
            visit((SQLite3WindowFunctionFrameSpecBetween) expr);
        } else if (expr instanceof SQLite3TableReference) {
            visit((SQLite3TableReference) expr);
        } else if (expr instanceof SQLite3SetClause) {
            visit((SQLite3SetClause) expr);
        } else if (expr instanceof SQLite3ManuelPredicate) {
            visit((SQLite3ManuelPredicate) expr);
        } else {
            throw new AssertionError(expr);
        }
    }

    static String asString(SQLite3Expression expr) {
        if (expr == null) {
            throw new AssertionError();
        }
        SQLite3ToStringVisitor visitor = new SQLite3ToStringVisitor();
        if (expr instanceof SQLite3Select) {
            visitor.visit((SQLite3Select) expr, false);
        } else {
            visitor.visit(expr);
        }
        return visitor.get();
    }

    static String asExpectedValues(SQLite3Expression expr) {
        SQLite3ExpectedValueVisitor visitor = new SQLite3ExpectedValueVisitor();
        visitor.visit(expr);
        return visitor.get();
    }

}
