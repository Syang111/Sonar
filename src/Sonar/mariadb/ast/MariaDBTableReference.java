package Sonar.mariadb.ast;

import Sonar.mariadb.MariaDBSchema;
import Sonar.mysql.MySQLSchema;

public class MariaDBTableReference implements MariaDBExpression{

    private final MariaDBSchema.MariaDBTable table;

    public MariaDBTableReference(MariaDBSchema.MariaDBTable table) {
        this.table = table;
    }

    public MariaDBSchema.MariaDBTable getTable() {
        return table;
    }
}
