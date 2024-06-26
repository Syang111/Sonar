package Sonar.tidb.visitor;

import Sonar.Randomly;
import Sonar.common.visitor.ToStringVisitor;
import Sonar.tidb.ast.*;
import Sonar.tidb.ast.TiDBJoin.JoinType;

public class TiDBToStringVisitor extends ToStringVisitor<TiDBExpression> implements TiDBVisitor {

    @Override
    public void visitSpecific(TiDBExpression expr) {
        TiDBVisitor.super.visit(expr);
    }

    @Override
    public void visit(TiDBConstant c) {
        sb.append(c.toString());
    }

    public String getString() {
        return sb.toString();
    }

    @Override
    public void visit(TiDBColumnReference c) {
        if (c.getColumn().getTable() == null) {
            sb.append(c.getColumn().getName());
        } else {
            sb.append(c.getColumn().getFullQualifiedName());
        }
    }

    @Override
    public void visit(TiDBTableReference expr) {
        sb.append(expr.getTable().getName());
    }

    @Override
    public void visit(TiDBSelect select) {
        sb.append("SELECT ");
        if (select.getHint() != null) {
            sb.append("");
        }
        visit(select.getFetchColumns());
        sb.append(" FROM ");


        for (int i = 0; i < select.getFromList().size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            if (select.getFromList().get(i) instanceof TiDBTableReference) {
                sb.append(((TiDBTableReference) select.getFromList().get(i)).getTable().getName());
            } else {
                sb.append(("("));
                visit(select.getFromList().get(i));
                sb.append((") AS t"));
            }
        }

        if (!select.getFromList().isEmpty() && !select.getJoinList().isEmpty()) {
            sb.append(", ");
        }
        if (!select.getJoinList().isEmpty()) {
            visit(select.getJoinList());
        }
        if (select.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(select.getWhereClause());
        }
        if (!select.getGroupByExpressions().isEmpty()) {
            sb.append(" GROUP BY ");
            visit(select.getGroupByExpressions());
        }
        if (select.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(select.getHavingClause());
        }
        if (!select.getOrderByExpressions().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(select.getOrderByExpressions());
        }
    }

    @Override
    public void visit(TiDBFunctionCall call) {
        sb.append(call.getFunction());
        sb.append("(");
        visit(call.getArgs());
        sb.append(")");
    }

    public void visit(TiDBManuelPredicate predicate) {
        sb.append(predicate.getString());
    }

    @Override
    public void visit(TiDBJoin join) {
        sb.append(" ");
        visit(join.getLeftTable());
        sb.append(" ");
        switch (join.getJoinType()) {
            case INNER:
                sb.append("INNER ");
                sb.append("JOIN ");
                break;
            case LEFT:
                sb.append("LEFT ");
                if (Randomly.getBoolean()) {
                    sb.append(" OUTER ");
                }
                sb.append("JOIN ");
                break;
            case RIGHT:
                sb.append("RIGHT ");
                if (Randomly.getBoolean()) {
                    sb.append(" OUTER ");
                }
                sb.append("JOIN ");
                break;
            case STRAIGHT:
                sb.append("STRAIGHT_JOIN ");
                break;
            case NATURAL:
                sb.append("NATURAL ");
                switch (join.getNaturalJoinType()) {
                    case INNER:
                        break;
                    case LEFT:
                        sb.append("LEFT ");
                        break;
                    case RIGHT:
                        sb.append("RIGHT ");
                        break;
                    default:
                        throw new AssertionError();
                }
                sb.append("JOIN ");
                break;
            case CROSS:
                sb.append("CROSS JOIN ");
                break;
            default:
                throw new AssertionError();
        }
        visit(join.getRightTable());
        if (join.getOnCondition() != null && join.getJoinType() != JoinType.NATURAL) {
            sb.append(" ON ");
            visit(join.getOnCondition());
        }
    }

    public void visit(TiDBDateExpression date) {
        sb.append(date.getDateExp());
    }

    public void visit(TiDBUnaryPostfixOperation op) {
        sb.append("(");
        visit(op.getExpression());
        sb.append(") ");
        sb.append(op.getOperatorRepresentation());
    }

    public void visit(TiDBUnaryPrefixOperation op) {
        sb.append(op.getOperatorRepresentation());
        sb.append(" (");
        visit(op.getExpression());
        sb.append(") ");
    }

    public void visit(TiDBBinaryLogicalOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(")");
        sb.append(" ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" ");
        sb.append("(");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(TiDBBinaryComparisonOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(TiDBBinaryArithmeticOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(TiDBRegexOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }

    @Override
    public void visit(TiDBBinaryBitOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(") ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" (");
        visit(op.getRight());
        sb.append(")");
    }


    @Override
    public void visit(TiDBPostfixText PostfixText) {
        sb.append("(");
        visit(PostfixText.getExpr());
        sb.append(")");
        sb.append(" AS ");
        sb.append(PostfixText.getText());

    }

    @Override
    public void visit(TiDBText text) {
        sb.append(text.getText());
    }

    @Override
    public void visit(TiDBAggregate aggr) {
        sb.append(aggr.getFunction());
        sb.append("(");
        visit(aggr.getArgs());
        sb.append(")");
    }

    @Override
    public void visit(TiDBCastOperation cast) {
        sb.append("CAST(");
        visit(cast.getExpr());
        sb.append(" AS ");
        sb.append(cast.getType());
        sb.append(")");
    }

    @Override
    public void visit(TiDBCase op) {
        sb.append("(CASE ");
        visit(op.getSwitchCondition());
        for (int i = 0; i < op.getConditions().size(); i++) {
            sb.append(" WHEN ");
            visit(op.getConditions().get(i));
            sb.append(" THEN ");
            visit(op.getExpressions().get(i));
        }
        if (op.getElseExpr() != null) {
            sb.append(" ELSE ");
            visit(op.getElseExpr());
        }
        sb.append(" END )");
    }
}
