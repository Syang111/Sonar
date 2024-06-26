package Sonar.mariadb.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Sonar.mariadb.MariaDBSchema.MariaDBTable;

public class MariaDBSelectStatement implements MariaDBExpression {

    public enum MariaDBSelectType {
        ALL,DISTINCT
    }

    List<MariaDBExpression> joinList = Collections.emptyList();
    private List<MariaDBExpression> groupBys = new ArrayList<>();
    private List<MariaDBExpression> columns = new ArrayList<>();
    private List<MariaDBExpression> tables = new ArrayList<>();
    private MariaDBSelectType selectType = MariaDBSelectType.ALL;
    private MariaDBExpression whereCondition;

    public void setGroupByClause(List<MariaDBExpression> groupBys) {
        this.groupBys = groupBys;
    }

    public void setFetchColumns(List<MariaDBExpression> columns) {
        this.columns = columns;

    }

    public void setFromTables(List<MariaDBExpression> tables) {
        this.tables = tables;
    }


    public void setSelectType(MariaDBSelectType selectType) {
        this.selectType = selectType;
    }

    public void setWhereClause(MariaDBExpression whereCondition) {
        this.whereCondition = whereCondition;
    }

    public List<MariaDBExpression> getColumns() {
        return columns;
    }

    public List<MariaDBExpression> getGroupBys() {
        return groupBys;
    }

    public MariaDBSelectType getSelectType() {
        return selectType;
    }

    public List<MariaDBExpression> getTables() {
        return tables;
    }

    public MariaDBExpression getWhereCondition() {
        return whereCondition;
    }

    public void setJoinList(List<MariaDBExpression> joinList) {
        this.joinList = joinList;
    }

    public List<MariaDBExpression> getJoinList() {
        return joinList;
    }
}
