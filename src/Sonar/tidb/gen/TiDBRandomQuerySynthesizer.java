package Sonar.tidb.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.tidb.TiDBExpressionGenerator;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.TiDBSchema.TiDBTables;
import Sonar.tidb.ast.TiDBExpression;
import Sonar.tidb.ast.TiDBSelect;
import Sonar.tidb.ast.TiDBTableReference;
import Sonar.tidb.visitor.TiDBVisitor;

public final class TiDBRandomQuerySynthesizer {

    private TiDBRandomQuerySynthesizer() {
    }

    public static SQLQueryAdapter generate(TiDBGlobalState globalState, int nrColumns) {
        TiDBSelect select = generateSelect(globalState, nrColumns);
        return new SQLQueryAdapter(TiDBVisitor.asString(select));
    }

    public static TiDBSelect generateSelect(TiDBGlobalState globalState, int nrColumns) {
        TiDBTables tables = globalState.getSchema().getRandomTableNonEmptyTables();
        TiDBExpressionGenerator gen = new TiDBExpressionGenerator(globalState).setColumns(tables.getColumns());
        TiDBSelect select = new TiDBSelect();

        List<TiDBExpression> columns = new ArrayList<>();

        columns.addAll(gen.generateExpressions(nrColumns));
        select.setFetchColumns(columns);
        List<TiDBExpression> tableList = tables.getTables().stream().map(t -> new TiDBTableReference(t))
                .collect(Collectors.toList());

        select.setFromList(tableList);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateHavingClause());
            }
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(gen.generateExpression());
        }
        if (Randomly.getBoolean()) {
            select.setOffsetClause(gen.generateExpression());
        }
        return select;
    }

}
