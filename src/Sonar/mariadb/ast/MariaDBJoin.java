package Sonar.mariadb.ast;

import Sonar.mariadb.MariaDBSchema;
import Sonar.mariadb.ast.MariaDBConstant;
import Sonar.mariadb.ast.MariaDBExpression;
import Sonar.mariadb.ast.MariaDBJoin;

public class MariaDBJoin implements MariaDBExpression{
    public enum JoinType {
        INNER, CROSS, NATURAL, RIGHT, LEFT;
    }

    private final MariaDBSchema.MariaDBTable table;
    private MariaDBExpression onClause;
    private MariaDBJoin.JoinType type;

    public MariaDBJoin(MariaDBJoin other) {
        this.table = other.table;
        this.onClause = other.onClause;
        this.type = other.type;
    }

    public MariaDBJoin(MariaDBSchema.MariaDBTable table, MariaDBExpression onClause, MariaDBJoin.JoinType type) {
        this.table = table;
        this.onClause = onClause;
        this.type = type;
    }

    public MariaDBJoin(MariaDBSchema.MariaDBTable table, MariaDBJoin.JoinType type) {
        this.table = table;
        if (type != MariaDBJoin.JoinType.NATURAL) {
            throw new AssertionError();
        }
        this.onClause = null;
        this.type = type;
    }

    public MariaDBSchema.MariaDBTable getTable() {
        return table;
    }

    public MariaDBExpression getOnClause() {
        return onClause;
    }

    public MariaDBJoin.JoinType getType() {
        return type;
    }

    public void setOnClause(MariaDBExpression onClause) {
        this.onClause = onClause;
    }

    public void setType(MariaDBJoin.JoinType type) {
        this.type = type;
    }
}
