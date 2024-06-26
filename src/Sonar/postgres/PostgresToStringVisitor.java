package Sonar.postgres;

import java.util.Optional;

import Sonar.Randomly;
import Sonar.common.visitor.BinaryOperation;
import Sonar.common.visitor.ToStringVisitor;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.ast.*;
import Sonar.postgres.ast.PostgresJoin.PostgresJoinType;
import Sonar.postgres.ast.PostgresSelect.PostgresFromTable;
import Sonar.postgres.ast.PostgresSelect.PostgresSubquery;

public final class PostgresToStringVisitor extends ToStringVisitor<PostgresExpression> implements PostgresVisitor {

    @Override
    public void visitSpecific(PostgresExpression expr) {
        PostgresVisitor.super.visit(expr);
    }

    @Override
    public void visit(PostgresConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(PostgresColumnReference column) {
        sb.append(column.getColumn().getFullQualifiedName());
    }

    @Override
    public void visit(PostgresPostfixOperation op) {
        sb.append("(");
        visit(op.getExpression());
        sb.append(")");
        sb.append(" ");
        sb.append(op.getOperatorTextRepresentation());
    }

    @Override
    public void visit(PostgresColumnValue c) {
        sb.append(c.getColumn().getFullQualifiedName());
    }

    @Override
    public void visit(PostgresPrefixOperation op) {
        sb.append(op.getTextRepresentation());
        sb.append(" (");
        visit(op.getExpression());
        sb.append(")");
    }

    public void visit(PostgresManuelPredicate p) {
        sb.append(p.getString());
    }
    @Override
    public void visit(PostgresFromTable from) {
        if (from.isOnly()) {
            sb.append("ONLY ");
        }
        sb.append(from.getTable().getName());
        if (!from.isOnly() && Randomly.getBoolean()) {
            sb.append("*");
        }
    }

    @Override
    public void visit(PostgresSubquery subquery) {
        sb.append("(");
        visit(subquery.getSelect());
        sb.append(") AS ");
        sb.append(subquery.getName());
    }

    @Override
    public void visit(PostgresTableReference ref) {
        sb.append(ref.getTable().getName());
    }

    @Override
    public void visit(PostgresSelect s) {
        sb.append("SELECT ");
        switch (s.getSelectOption()) {
        case DISTINCT:
            sb.append("DISTINCT ");
            if (s.getDistinctOnClause() != null) {
                sb.append("ON (");
                visit(s.getDistinctOnClause());
                sb.append(") ");
            }
            break;
        case ALL:
            sb.append(Randomly.fromOptions("ALL ", ""));
            break;
        default:
            throw new AssertionError();
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
            if (s.getFromList().get(i) instanceof PostgresFromTable){
                sb.append(((PostgresFromTable)s.getFromList().get(i)).getTable().getName());
            }else {
                sb.append(("("));
                visit(s.getFromList().get(i));
                sb.append((") AS t"));
            }
        }

        for (PostgresJoin j : s.getJoinClauses()) {
            sb.append(" ");
            switch (j.getType()) {
            case INNER:
                if (Randomly.getBoolean()) {
                    sb.append("INNER ");
                }
                sb.append("JOIN");
                break;
            case LEFT:
                sb.append("LEFT OUTER JOIN");
                break;
            case RIGHT:
                sb.append("RIGHT OUTER JOIN");
                break;
            case FULL:
                sb.append("FULL OUTER JOIN");
                break;
            case CROSS:
                sb.append("CROSS JOIN");
                break;
            default:
                throw new AssertionError(j.getType());
            }
            sb.append(" ");
            visit(j.getTableReference());
            if (j.getType() != PostgresJoinType.CROSS) {
                sb.append(" ON ");
                visit(j.getOnClause());
            }
        }

        if (s.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(s.getWhereClause());
        }
        if (s.getGroupByExpressions().size() > 0) {
            sb.append(" GROUP BY ");
            visit(s.getGroupByExpressions());
        }
        if (s.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(s.getHavingClause());

        }
        if (!s.getOrderByExpressions().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(s.getOrderByExpressions());
        }
        if (s.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(s.getLimitClause());
        }

        if (s.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(s.getOffsetClause());
        }
    }

    @Override
    public void visit(PostgresOrderByTerm op) {
        visit(op.getExpr());
        sb.append(" ");
        sb.append(op.getOrder());
    }

    @Override
    public void visit(PostgresFunction f) {
        sb.append(f.getFunctionName());
        sb.append("(");
        int i = 0;
        for (PostgresExpression arg : f.getArguments()) {
            if (i++ != 0) {
                sb.append(", ");
            }
            visit(arg);
        }
        sb.append(")");
    }

    @Override
    public void visit(PostgresCastOperation cast) {
        if (Randomly.getBoolean()) {
            sb.append("CAST(");
            visit(cast.getExpression());
            sb.append(" AS ");
            appendType(cast);
            sb.append(")");
        } else {
            sb.append("(");
            visit(cast.getExpression());
            sb.append(")::");
            appendType(cast);
        }
    }

    private void appendType(PostgresCastOperation cast) {
        PostgresCompoundDataType compoundType = cast.getCompoundType();
        switch (compoundType.getDataType()) {
        case BOOLEAN:
            sb.append("BOOLEAN");
            break;
        case INT:
            sb.append("INT");
            break;
        case TEXT:

            sb.append(Randomly.fromOptions("VARCHAR"));
            break;
        case REAL:
            sb.append("FLOAT");
            break;
        case DECIMAL:
            sb.append("DECIMAL");
            break;
        case FLOAT:
            sb.append("REAL");
            break;
        case RANGE:
            sb.append("int4range");
            break;
        case MONEY:
            sb.append("MONEY");
            break;
        case INET:
            sb.append("INET");
            break;
        case BIT:
            sb.append("BIT");





            break;
        default:
            throw new AssertionError(cast.getType());
        }
        Optional<Integer> size = compoundType.getSize();
        if (size.isPresent()) {
            sb.append("(");
            sb.append(size.get());
            sb.append(")");
        }
    }

    @Override
    public void visit(PostgresBetweenOperation op) {
        sb.append("(");
        visit(op.getExpr());
        if (PostgresProvider.generateOnlyKnown && op.getExpr().getExpressionType() == PostgresDataType.TEXT
                && op.getLeft().getExpressionType() == PostgresDataType.TEXT) {
            sb.append(" COLLATE \"C\"");
        }
        sb.append(") BETWEEN ");
        if (op.isSymmetric()) {
            sb.append("SYMMETRIC ");
        }
        sb.append("(");
        visit(op.getLeft());
        sb.append(") AND (");
        visit(op.getRight());
        if (PostgresProvider.generateOnlyKnown && op.getExpr().getExpressionType() == PostgresDataType.TEXT
                && op.getRight().getExpressionType() == PostgresDataType.TEXT) {
            sb.append(" COLLATE \"C\"");
        }
        sb.append(")");
    }

    @Override
    public void visit(PostgresInOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(")");
        if (!op.isTrue()) {
            sb.append(" NOT");
        }
        sb.append(" IN (");
        visit(op.getListElements());
        sb.append(")");
    }

    @Override
    public void visit(PostgresPostfixText op) {


        sb.append("(");
        visit(op.getExpr());
        sb.append(")");
        sb.append(" AS ");
        sb.append(op.getText());
    }

    @Override
    public void visit(PostgresAggregate op) {
        sb.append(op.getFunction());
        sb.append("(");
        visit(op.getArgs());
        sb.append(")");
    }

    @Override
    public void visit(PostgresSimilarTo op) {
        sb.append("(");
        visit(op.getString());
        sb.append(" SIMILAR TO ");
        visit(op.getSimilarTo());
        if (op.getEscapeCharacter() != null) {
            visit(op.getEscapeCharacter());
        }
        sb.append(")");
    }

    @Override
    public void visit(PostgresPOSIXRegularExpression op) {
        visit(op.getString());
        sb.append(op.getOp().getStringRepresentation());
        visit(op.getRegex());
    }

    @Override
    public void visit(PostgresCollate op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(" COLLATE ");
        sb.append('"');
        sb.append(op.getCollate());
        sb.append('"');
        sb.append(")");
    }

    @Override
    public void visit(PostgresBinaryLogicalOperation op) {
        super.visit((BinaryOperation<PostgresExpression>) op);
    }

    @Override
    public void visit(PostgresLikeOperation op) {
        super.visit((BinaryOperation<PostgresExpression>) op);
    }

}
