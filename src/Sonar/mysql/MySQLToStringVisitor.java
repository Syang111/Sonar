package Sonar.mysql;

import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.common.visitor.ToStringVisitor;
import Sonar.mysql.ast.*;
import Sonar.mysql.ast.MySQLOrderByTerm.MySQLOrder;


public class MySQLToStringVisitor extends ToStringVisitor<MySQLExpression> implements MySQLVisitor {

    int ref;

    @Override
    public void visitSpecific(MySQLExpression expr) {
        MySQLVisitor.super.visit(expr);
    }

    @Override
    public void visit(MySQLSelect s,boolean inner) {
        if(inner){
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
        case DISTINCTROW:
            sb.append("DISTINCTROW ");
            break;
        default:
            throw new AssertionError();
        }
        sb.append(s.getModifiers().stream().collect(Collectors.joining(" ")));
        if (s.getModifiers().size() > 0) {
            sb.append(" ");
        }
        if (s.getFetchColumns() == null) {
            sb.append("*");
        } else {
            for (int i = 0; i < s.getFetchColumns().size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                visit(s.getFetchColumns().get(i));




            }
        }
        sb.append(" FROM ");
        for (int i = 0; i < s.getFromList().size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            visit(s.getFromList().get(i));
        }

        for (MySQLExpression j : s.getJoinList()) {
            visit(j);
        }

        if (s.getWhereClause() != null) {
            MySQLExpression whereClause = s.getWhereClause();
            sb.append(" WHERE ");
            visit(whereClause);
        }
        if (s.getGroupByExpressions() != null && s.getGroupByExpressions().size() > 0) {
            sb.append(" ");
            sb.append("GROUP BY ");
            List<MySQLExpression> groupBys = s.getGroupByExpressions();
            for (int i = 0; i < groupBys.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                visit(groupBys.get(i));
            }
        }

        if (s.getHavingClause() != null) {
            sb.append(" ");
            sb.append("HAVING ");
            visit(s.getHavingClause());
        }
        if (!s.getOrderByExpressions().isEmpty()) {
            sb.append(" ORDER BY ");
            List<MySQLExpression> orderBys = s.getOrderByExpressions();
            for (int i = 0; i < orderBys.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                visit(s.getOrderByExpressions().get(i));
            }
        }
        if (s.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(s.getLimitClause());
        }

        if (s.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(s.getOffsetClause());
        }
        if(inner){
            sb.append(") as t");
        }
    }

    public void visit(MySQLWindowFunction window) {
        sb.append(window.getFunction().getName());
        sb.append("(");
        if(window.getExpr() != null) {
            visit(window.getExpr());
        }
        sb.append(")");
        sb.append(" OVER (PARTITION BY ");
        visit(window.getPartitionBy());
        sb.append(")");
    }

    public void visit(MySQLJoin join) {
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
            case LEFT:
                sb.append("LEFT OUTER");
                break;
            case RIGHT:
                sb.append("RIGHT OUTER");
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

    public void visit(MySQLManuelPredicate p) {
        sb.append(p.getString());
    }
    @Override
    public void visit(MySQLPostfixText PostfixText) {
        sb.append("(");
        visit(PostfixText.getExpr());
        sb.append(")");
        sb.append(" AS ");
        sb.append(PostfixText.getText());

    }
    @Override
    public void visit(MySQLDataExpression date) {
        sb.append(date.getDateExp());
    }
    @Override
    public void visit(MySQLConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    public void visit(MySQLAggregateFunction aggr) {
        sb.append(aggr.getFunction().getName());
        sb.append("(");
        visit(aggr.getExpr());
        sb.append(")");
    }
    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(MySQLColumnReference column) {
        sb.append(column.getColumn().getFullQualifiedName() );
    }

    @Override
    public void visit(MySQLUnaryPostfixOperation op) {
        sb.append("(");
        visit(op.getExpression());
        sb.append(")");
        sb.append(" IS ");
        if (op.isNegated()) {
            sb.append("NOT ");
        }
        switch (op.getOperator()) {
        case IS_FALSE:
            sb.append("FALSE");
            break;
        case IS_NULL:
            if (Randomly.getBoolean()) {
                sb.append("UNKNOWN");
            } else {
                sb.append("NULL");
            }
            break;
        case IS_TRUE:
            sb.append("TRUE");
            break;
        default:
            throw new AssertionError(op);
        }
    }

    @Override
    public void visit(MySQLComputableFunction f) {
        sb.append(f.getFunction().getName());
        sb.append("(");
        for (int i = 0; i < f.getArguments().length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            visit(f.getArguments()[i]);
        }
        sb.append(")");
    }

    @Override
    public void visit(MySQLBinaryLogicalOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(")");
        sb.append(" ");
        sb.append(op.getTextRepresentation());
        sb.append(" ");
        sb.append("(");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(MySQLBinaryComparisonOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(MySQLBinaryArithOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(MySQLCastOperation op) {
        sb.append("CAST(");
        visit(op.getExpr());
        sb.append(" AS ");
        sb.append(op.getType());
        sb.append(")");
    }

    @Override
    public void visit(MySQLInOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(")");
        if (!op.isTrue()) {
            sb.append(" NOT");
        }
        sb.append(" IN ");
        sb.append("(");
        for (int i = 0; i < op.getListElements().size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            visit(op.getListElements().get(i));
        }
        sb.append(")");
    }

    @Override
    public void visit(MySQLBinaryOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(MySQLOrderByTerm op) {
        visit(op.getExpr());
        sb.append(" ");
        sb.append(op.getOrder() == MySQLOrder.ASC ? "ASC" : "DESC");
    }

    @Override
    public void visit(MySQLExists op) {
        sb.append(" EXISTS (");
        visit(op.getExpr());
        sb.append(")");
    }

    @Override
    public void visit(MySQLStringExpression op) {
        sb.append(op.getStr());
    }

    @Override
    public void visit(MySQLBetweenOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(") BETWEEN (");
        visit(op.getLeft());
        sb.append(") AND (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(MySQLTableReference ref) {
        sb.append(ref.getTable().getName());
    }

    @Override
    public void visit(MySQLCollate collate) {
        sb.append("(");
        visit(collate.getExpression());
        sb.append(" ");
        sb.append(collate.getOperatorRepresentation());
        sb.append(")");
    }

}
