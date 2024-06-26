package Sonar.sqlite3;

import java.util.Arrays;

import Sonar.Randomly;
import Sonar.common.visitor.ToStringVisitor;
import Sonar.sqlite3.ast.SQLite3Aggregate;
import Sonar.sqlite3.ast.SQLite3Aggregate.SQLite3AggregateFunction;
import Sonar.sqlite3.ast.SQLite3Case.CasePair;
import Sonar.sqlite3.ast.SQLite3Case.SQLite3CaseWithBaseExpression;
import Sonar.sqlite3.ast.SQLite3Case.SQLite3CaseWithoutBaseExpression;
import Sonar.sqlite3.ast.SQLite3Cast;
import Sonar.sqlite3.ast.SQLite3Constant;
import Sonar.sqlite3.ast.SQLite3Expression;
import Sonar.sqlite3.ast.SQLite3Expression.BetweenOperation;
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
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3TableReference;
import Sonar.sqlite3.ast.SQLite3Expression.SQLite3Text;
import Sonar.sqlite3.ast.SQLite3Expression.Subquery;
import Sonar.sqlite3.ast.SQLite3Expression.TypeLiteral;
import Sonar.sqlite3.ast.SQLite3Function;
import Sonar.sqlite3.ast.SQLite3RowValueExpression;
import Sonar.sqlite3.ast.SQLite3Select;
import Sonar.sqlite3.ast.SQLite3SetClause;
import Sonar.sqlite3.ast.SQLite3WindowFunction;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression.SQLite3WindowFunctionFrameSpecBetween;
import Sonar.sqlite3.ast.SQLite3WindowFunctionExpression.SQLite3WindowFunctionFrameSpecTerm;

public class SQLite3ToStringVisitor extends ToStringVisitor<SQLite3Expression> implements SQLite3Visitor {

    public boolean fullyQualifiedNames = true;

    @Override
    public void visitSpecific(SQLite3Expression expr) {
        SQLite3Visitor.super.visit(expr);
    }

    protected void asHexString(long intVal) {
        String hexVal = Long.toHexString(intVal);
        String prefix;
        if (Randomly.getBoolean()) {
            prefix = "0x";
        } else {
            prefix = "0X";
        }
        sb.append(prefix);
        sb.append(hexVal);
    }

    @Override
    public void visit(BetweenOperation op) {
        sb.append("(");
        sb.append("(");
        visit(op.getExpression());
        sb.append(")");
        if (op.isNegated()) {
            sb.append(" NOT");
        }
        sb.append(" BETWEEN ");
        sb.append("(");
        visit(op.getLeft());
        sb.append(")");
        sb.append(" AND ");
        sb.append("(");
        visit(op.getRight());
        sb.append(")");
        sb.append(")");
    }

    @Override
    public void visit(SQLite3ColumnName c) {




        sb.append(c.getColumn().getName());
    }

    @Override
    public void visit(Function f) {
        sb.append(f.getName());
        sb.append("(");
        visit(f.getArguments());
        sb.append(")");
    }

    @Override
    public void visit(SQLite3Select s, boolean inner) {
        if (inner) {
            sb.append("(");
        }
        sb.append("SELECT ");
        switch (s.getFromOptions()) {
        case DISTINCT:
            sb.append("DISTINCT ");
            break;
        case ALL:
            sb.append(Randomly.fromOptions("ALL ", ""));
            break;
        default:
            throw new AssertionError(s.getFromOptions());
        }
        if (s.getFetchColumns() == null) {
            sb.append("*");
        } else {
            visit(s.getFetchColumns());
        }
        sb.append(" FROM ");
        for (int i = 0; i < s.getFromList().size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            visit(s.getFromList().get(i));
        }
        for (Join j : s.getJoinClauses()) {
            visit(j);
        }

        if (s.getWhereClause() != null) {
            SQLite3Expression whereClause = s.getWhereClause();
            sb.append(" WHERE (");
            visit(whereClause);
            sb.append(")");
        }
        if (s.getGroupByClause().size() > 0) {
            sb.append(" ");
            sb.append("GROUP BY ");
            visit(s.getGroupByClause());
        }
        if (s.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(s.getHavingClause());
        }
        if (!s.getOrderByClause().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(s.getOrderByClause());
        }
        if (s.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(s.getLimitClause());
        }

        if (s.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(s.getOffsetClause());
        }
        if (inner) {
            sb.append(")");
        }
    }

    @Override
    public void visit(SQLite3Constant c) {
        if (c.isNull()) {
            sb.append("NULL");
        } else {
            switch (c.getDataType()) {
            case INT:




                if (!c.isHex() || c.asInt() == Long.MIN_VALUE) {
                    sb.append(c.asInt());
                } else {
                    long intVal = c.asInt();
                    asHexString(intVal);
                }

                break;
            case REAL:
                double asDouble = c.asDouble();
                if (Double.POSITIVE_INFINITY == asDouble) {
                    sb.append("1e500");
                } else if (Double.NEGATIVE_INFINITY == asDouble) {
                    sb.append("-1e500");
                } else if (Double.isNaN(asDouble)) {

                    sb.append("1e500 / 1e500");
                } else {
                    sb.append(asDouble);
                }
                break;
            case TEXT:
                sb.append("'");
                sb.append(c.asString().replace("'", "''"));
                sb.append("'");
                break;
            case BINARY:
                sb.append('x');
                sb.append("'");
                byte[] arr;
                if (c.getValue() instanceof byte[]) {
                    arr = c.asBinary();
                } else {
                    arr = c.asString().getBytes(SQLite3Cast.DEFAULT_ENCODING);
                }
                sb.append(SQLite3Visitor.byteArrayToHex(arr));
                sb.append("'");
                break;
            default:
                throw new AssertionError(c.getDataType());
            }
        }
    }

    @Override
    public void visit(Join join) {
        sb.append(" ");
        switch (join.getType()) {
        case CROSS:
            sb.append("CROSS");
            break;
        case INNER:
            sb.append("INNER");
            break;
        case NATURAL:
            sb.append("NATURAL");
            break;
        case OUTER:
            sb.append("LEFT OUTER");
            break;
        case RIGHT:
            sb.append("RIGHT OUTER");
            break;
        case FULL:
            sb.append("FULL OUTER");
            break;
        default:
            throw new AssertionError(join.getType());
        }
        sb.append(" JOIN ");
        sb.append(join.getTable().getName());
        if (join.getOnClause() != null) {
            sb.append(" ON ");
            visit(join.getOnClause());
        }
    }

    @Override
    public void visit(SQLite3OrderingTerm term) {
        visit(term.getExpression());

        sb.append(" ");
        sb.append(term.getOrdering().toString());
    }

    @Override
    public void visit(CollateOperation op) {
        visit(op.getExpression());
        sb.append(" COLLATE ");
        sb.append(op.getCollate());
    }

    @Override
    public void visit(Cast cast) {
        sb.append("CAST(");
        visit(cast.getExpression());
        sb.append(" AS ");
        visit(cast.getType());
        sb.append(")");
    }

    @Override
    public void visit(TypeLiteral literal) {
        sb.append(literal.getType());
    }

    @Override
    public void visit(InOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(" IN ");
        sb.append("(");
        if (op.getRightExpressionList() != null) {
            visit(op.getRightExpressionList());
        } else {
            visit(op.getRightSelect());
        }
        sb.append(")");
        sb.append(")");
    }

    @Override
    public void visit(Subquery query) {
        sb.append(query.getQuery());
    }

    @Override
    public void visit(SQLite3Exist exist) {
        sb.append(" EXISTS ");
        if (exist.getExpression() instanceof SQLite3SetClause) {
            sb.append("(");
        }
        visit(exist.getExpression());
        if (exist.getExpression() instanceof SQLite3SetClause) {
            sb.append(")");
        }
        sb.append("");
    }

    @Override
    public void visit(SQLite3Aggregate aggr) {
        if (aggr.getFunc() == SQLite3AggregateFunction.COUNT_ALL) {
            sb.append("COUNT(*)");
        } else {
            sb.append(aggr.getFunc());
            sb.append("(");
            visit(aggr.getExpr());
            sb.append(")");
        }
    }

    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(SQLite3Function func) {
        sb.append(func.getFunc());
        sb.append("(");
        visit(func.getArgs());
        sb.append(")");
    }

    @Override
    public void visit(SQLite3Distinct distinct) {
        sb.append("DISTINCT ");
        visit(distinct.getExpression());
    }

    @Override
    public void visit(SQLite3CaseWithoutBaseExpression casExpr) {
        sb.append("CASE");
        for (CasePair pair : casExpr.getPairs()) {
            sb.append(" WHEN ");
            visit(pair.getCond());
            sb.append(" THEN ");
            visit(pair.getThen());
        }
        if (casExpr.getElseExpr() != null) {
            sb.append(" ELSE ");
            visit(casExpr.getElseExpr());
        }
        sb.append(" END");
    }

    @Override
    public void visit(SQLite3CaseWithBaseExpression casExpr) {
        sb.append("CASE ");
        visit(casExpr.getBaseExpr());
        sb.append(" ");
        for (CasePair pair : casExpr.getPairs()) {
            sb.append(" WHEN ");
            visit(pair.getCond());
            sb.append(" THEN ");
            visit(pair.getThen());
        }
        if (casExpr.getElseExpr() != null) {
            sb.append(" ELSE ");
            visit(casExpr.getElseExpr());
        }
        sb.append(" END");
    }

    @Override
    public void visit(SQLite3WindowFunction func) {
        sb.append(func.getFunc());
        sb.append("(");
        visit(func.getArgs());
        sb.append(")");
    }

    @Override
    public void visit(MatchOperation match) {
        visit(match.getLeft());
        sb.append(" MATCH ");
        visit(match.getRight());
    }

    @Override
    public void visit(SQLite3RowValueExpression rw) {
        sb.append("(");
        visit(rw.getExpressions());
        sb.append(")");
    }

    @Override
    public void visit(SQLite3Text func) {
        sb.append(func.getText());
    }

    @Override
    public void visit(SQLite3WindowFunctionExpression windowFunction) {
        visit(windowFunction.getBaseWindowFunction());
        if (windowFunction.getFilterClause() != null) {
            sb.append(" FILTER(WHERE ");
            visit(windowFunction.getFilterClause());
            sb.append(")");
        }
        sb.append(" OVER (");
        if (!windowFunction.getPartitionBy().isEmpty()) {
            sb.append(" PARTITION BY ");
            visit(windowFunction.getPartitionBy());
        }
        if (!windowFunction.getOrderBy().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(windowFunction.getOrderBy());
        }
        if (windowFunction.getFrameSpec() != null) {
            sb.append(" ");
            sb.append(windowFunction.getFrameSpecKind());
            sb.append(" ");
            visit(windowFunction.getFrameSpec());
            if (windowFunction.getExclude() != null) {
                sb.append(" ");
                sb.append(windowFunction.getExclude().getString());
            }
        }
        sb.append(")");
    }

    @Override
    public void visit(SQLite3WindowFunctionFrameSpecTerm term) {
        if (term.getExpression() != null) {
            visit(term.getExpression());
        }
        sb.append(" ");
        sb.append(term.getKind().getString());
    }

    @Override
    public void visit(SQLite3WindowFunctionFrameSpecBetween between) {
        sb.append("BETWEEN ");
        visit(between.getLeft());
        sb.append(" AND ");
        visit(between.getRight());
    }

    @Override
    public void visit(SQLite3TableReference tableReference) {
        sb.append(tableReference.getTable().getName());








    }

    private void visit(SQLite3Expression... expressions) {
        visit(Arrays.asList(expressions));
    }

    @Override
    public void visit(SQLite3SetClause set) {

        sb.append(SQLite3Visitor.asString(set.getLeft()));
        sb.append(" ");
        sb.append(set.getType().getTextRepresentation());
        sb.append(" ");
        sb.append(SQLite3Visitor.asString(set.getRight()));
    }

    public void visit(SQLite3Expression.SQLite3ManuelPredicate predicate){
        sb.append(predicate.getString());
    }

}
