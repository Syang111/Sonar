package Sonar.mariadb.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Sonar.Randomly;
import Sonar.SQLConnection;
import Sonar.StateToReproduce;
import Sonar.mariadb.MariaDBProvider;
import Sonar.mariadb.MariaDBSchema;
import Sonar.mariadb.MariaDBSchema.MariaDBColumn;
import Sonar.mariadb.MariaDBSchema.MariaDBDataType;
import Sonar.mariadb.ast.*;
import Sonar.mariadb.ast.MariaDBBinaryOperator.MariaDBBinaryComparisonOperator;
import Sonar.mariadb.ast.MariaDBPostfixUnaryOperation.MariaDBPostfixUnaryOperator;
import Sonar.mariadb.ast.MariaDBUnaryPrefixOperation.MariaDBUnaryPrefixOperator;



public class MariaDBExpressionGenerator{

    private final Randomly r;
    private List<MariaDBColumn> columns = new ArrayList<>();

    public MariaDBExpressionGenerator(Randomly r) {
        this.r = r;
    }

    public static MariaDBConstant getRandomConstant(Randomly r) {
        MariaDBDataType option = Randomly.fromOptions(MariaDBDataType.values());
        return getRandomConstant(r, option);
    }

    public static MariaDBConstant getRandomConstant(Randomly r, MariaDBDataType option) throws AssertionError {
        if (Randomly.getBooleanWithSmallProbability()) {
            return MariaDBConstant.createNullConstant();
        }
        switch (option) {
        case REAL:

            return MariaDBConstant.createIntConstant(r.getInteger());





        case INT:
            return MariaDBConstant.createIntConstant(r.getInteger());
        case VARCHAR:
            return MariaDBConstant.createTextConstant(r.getString());
        case BOOLEAN:
            return MariaDBConstant.createBooleanConstant(Randomly.getBoolean());
        default:
            throw new AssertionError(option);
        }
    }

    public MariaDBExpressionGenerator setColumns(List<MariaDBColumn> columns) {
        this.columns = columns;
        return this;
    }

    public MariaDBExpressionGenerator setCon(SQLConnection con) {
        return this;
    }

    public MariaDBExpressionGenerator setState(StateToReproduce state) {
        return this;
    }

    private enum ExpressionType {
        LITERAL, COLUMN, BINARY_COMPARISON, UNARY_POSTFIX_OPERATOR, UNARY_PREFIX_OPERATOR, FUNCTION, IN

    }

    private enum ColumnExpressionActions {
        BINARY_COMPARISON,
        COLUMN,
        

    }
    private enum  HavingColumnExpression {
        BINARY_COMPARISON,
    }

    public MariaDBExpression generateFetchColumnExpression() {
        switch (Randomly.fromOptions(MariaDBExpressionGenerator.ColumnExpressionActions.values())) {
            case BINARY_COMPARISON:
                return new MariaDBBinaryOperator(getRandomColumn(), getRandomExpression(2),
                        MariaDBBinaryComparisonOperator.getRandom());
            case COLUMN:
                return getRandomColumn();
            default:
                throw new AssertionError();
        }
    }

    public MariaDBExpression generateWhereColumnExpression(MariaDBPostfixText postfixText) {
        switch (Randomly.fromOptions(MariaDBExpressionGenerator.HavingColumnExpression.values())) {
            case BINARY_COMPARISON:
                return new MariaDBBinaryOperator(new MariaDBManuelPredicate(postfixText.getText()), getRandomConstant(new Randomly()),
                        MariaDBBinaryComparisonOperator.getRandom());
            default:
                throw new AssertionError();
        }
    }
    
    public MariaDBExpression getRandomExpression(int depth) {
        if (depth >= MariaDBProvider.MAX_EXPRESSION_DEPTH || Randomly.getBoolean()) {
            if (Randomly.getBoolean() || columns.isEmpty()) {
                return getRandomConstant(r);
            } else {
                return getRandomColumn();
            }
        }
        List<ExpressionType> expressionTypes = new ArrayList<>(Arrays.asList(ExpressionType.values()));
        if (columns.isEmpty()) {
            expressionTypes.remove(ExpressionType.COLUMN);
        }
        ExpressionType expressionType = Randomly.fromList(expressionTypes);
        switch (expressionType) {
        case COLUMN:
            getRandomColumn();


        case LITERAL:
            return getRandomConstant(r);
        case BINARY_COMPARISON:
            return new MariaDBBinaryOperator(getRandomExpression(depth + 1), getRandomExpression(depth + 1),
                    MariaDBBinaryComparisonOperator.getRandom());
        case UNARY_PREFIX_OPERATOR:
            return new MariaDBUnaryPrefixOperation(getRandomExpression(depth + 1),
                    MariaDBUnaryPrefixOperator.getRandom());
        case UNARY_POSTFIX_OPERATOR:
            return new MariaDBPostfixUnaryOperation(MariaDBPostfixUnaryOperator.getRandom(),
                    getRandomExpression(depth + 1));
        case FUNCTION:
            MariaDBFunctionName func = MariaDBFunctionName.getRandom();
            return new MariaDBFunction(func, getArgs(func, depth + 1));
        case IN:
            return new MariaDBInOperation(getRandomExpression(depth + 1), getSmallNumberRandomExpressions(depth + 1),
                    Randomly.getBoolean());
        default:
            throw new AssertionError(expressionType);
        }
    }

    public List<MariaDBExpression> getRandomJoinClauses(List<MariaDBSchema.MariaDBTable> tables) {
        List<MariaDBExpression> joinStatements = new ArrayList<>();
        List<MariaDBJoin.JoinType> options = new ArrayList<>(Arrays.asList(MariaDBJoin.JoinType.values()));
        if (tables.size() > 1) {
            int nrJoinClauses = (int) Randomly.getNotCachedInteger(0, tables.size());



            if (nrJoinClauses > 1) {
                options.remove(MariaDBJoin.JoinType.NATURAL);
            }
            for (int i = 0; i < nrJoinClauses; i++) {
                MariaDBSchema.MariaDBTable table = Randomly.fromList(tables);
                tables.remove(table);
                setColumns(table.getColumns());
                MariaDBExpression joinClause = getRandomExpression(2);
                MariaDBJoin.JoinType selectedOption = Randomly.fromList(options);
                if (selectedOption == MariaDBJoin.JoinType.NATURAL) {

                    joinClause = null;
                }
                MariaDBJoin j = new MariaDBJoin(table, joinClause, selectedOption);
                joinStatements.add(j);
            }
        }
        return joinStatements;
    }
    
    private List<MariaDBExpression> getSmallNumberRandomExpressions(int depth) {
        List<MariaDBExpression> expressions = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            expressions.add(getRandomExpression(depth + 1));
        }
        return expressions;
    }

    private List<MariaDBExpression> getArgs(MariaDBFunctionName func, int depth) {
        List<MariaDBExpression> expressions = new ArrayList<>();
        for (int i = 0; i < func.getNrArgs(); i++) {
            expressions.add(getRandomExpression(depth + 1));
        }
        if (func.isVariadic()) {
            for (int i = 0; i < Randomly.smallNumber(); i++) {
                expressions.add(getRandomExpression(depth + 1));
            }
        }
        return expressions;
    }

    public MariaDBExpression getRandomColumn() {
        MariaDBColumn randomColumn = Randomly.fromList(columns);
        return new MariaDBColumnName(randomColumn);
    }

    public MariaDBExpression getRandomExpression() {
        return getRandomExpression(0);
    }

}
