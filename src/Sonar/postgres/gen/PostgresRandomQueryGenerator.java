package Sonar.postgres.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Sonar.Randomly;
import Sonar.postgres.PostgresGlobalState;
import Sonar.postgres.PostgresSchema.PostgresDataType;
import Sonar.postgres.PostgresSchema.PostgresTables;
import Sonar.postgres.ast.PostgresConstant;
import Sonar.postgres.ast.PostgresExpression;
import Sonar.postgres.ast.PostgresSelect;
import Sonar.postgres.ast.PostgresSelect.ForClause;
import Sonar.postgres.ast.PostgresSelect.PostgresFromTable;
import Sonar.postgres.ast.PostgresSelect.SelectType;

public final class PostgresRandomQueryGenerator {

    private PostgresRandomQueryGenerator() {
    }

    public static PostgresSelect createRandomQuery(int nrColumns, PostgresGlobalState globalState) {
        List<PostgresExpression> columns = new ArrayList<>();
        PostgresTables tables = globalState.getSchema().getRandomTableNonEmptyTables();
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(globalState).setColumns(tables.getColumns());
        for (int i = 0; i < nrColumns; i++) {
            columns.add(gen.generateExpression(0));
        }
        PostgresSelect select = new PostgresSelect();
        select.setSelectType(SelectType.getRandom());
        if (select.getSelectOption() == SelectType.DISTINCT && Randomly.getBoolean()) {
            select.setDistinctOnClause(gen.generateExpression(0));
        }
        select.setFromList(tables.getTables().stream().map(t -> new PostgresFromTable(t, Randomly.getBoolean()))
                .collect(Collectors.toList()));
        select.setFetchColumns(columns);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(0, PostgresDataType.BOOLEAN));
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
            if (Randomly.getBoolean()) {
                select.setHavingClause(gen.generateHavingClause());
            }
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBy());
        }
        if (Randomly.getBoolean()) {
            select.setLimitClause(PostgresConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            if (Randomly.getBoolean()) {
                select.setOffsetClause(
                        PostgresConstant.createIntConstant(Randomly.getPositiveOrZeroNonCachedInteger()));
            }
        }
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setForClause(ForClause.getRandom());
        }
        return select;
    }

}
