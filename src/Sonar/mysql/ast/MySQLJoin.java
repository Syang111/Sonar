package Sonar.mysql.ast;

import Sonar.mysql.MySQLSchema.MySQLTable;

public class MySQLJoin implements MySQLExpression {

    public enum JoinType {
        INNER, CROSS, NATURAL, RIGHT, LEFT;
    }

    private final MySQLTable table;
    private MySQLExpression onClause;
    private MySQLJoin.JoinType type;

    public MySQLJoin(MySQLJoin other) {
        this.table = other.table;
        this.onClause = other.onClause;
        this.type = other.type;
    }

    public MySQLJoin(MySQLTable table, MySQLExpression onClause, MySQLJoin.JoinType type) {
        this.table = table;
        this.onClause = onClause;
        this.type = type;
    }

    public MySQLJoin(MySQLTable table, MySQLJoin.JoinType type) {
        this.table = table;
        if (type != MySQLJoin.JoinType.NATURAL) {
            throw new AssertionError();
        }
        this.onClause = null;
        this.type = type;
    }

    public MySQLTable getTable() {
        return table;
    }

    public MySQLExpression getOnClause() {
        return onClause;
    }

    public MySQLJoin.JoinType getType() {
        return type;
    }

    public void setOnClause(MySQLExpression onClause) {
        this.onClause = onClause;
    }

    public void setType(MySQLJoin.JoinType type) {
        this.type = type;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        throw new UnsupportedOperationException();
    }
}
