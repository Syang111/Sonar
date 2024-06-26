package Sonar.mysql.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Sonar.Randomly;
import Sonar.common.gen.UntypedExpressionGenerator;
import Sonar.mysql.MySQLGlobalState;
import Sonar.mysql.MySQLSchema;
import Sonar.mysql.MySQLSchema.MySQLColumn;
import Sonar.mysql.MySQLSchema.MySQLRowValue;
import Sonar.mysql.ast.*;
import Sonar.mysql.ast.MySQLBinaryComparisonOperation.BinaryComparisonOperator;
import Sonar.mysql.ast.MySQLBinaryLogicalOperation.MySQLBinaryLogicalOperator;
import Sonar.mysql.ast.MySQLBinaryOperation.MySQLBinaryOperator;
import Sonar.mysql.ast.MySQLComputableFunction.MySQLFunction;
import Sonar.mysql.ast.MySQLConstant.MySQLDoubleConstant;
import Sonar.mysql.ast.MySQLOrderByTerm.MySQLOrder;
import Sonar.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;
import Sonar.mysql.ast.MySQLBinaryArithOperation.MySQLBinaryArithOperator;


public class MySQLExpressionGenerator extends UntypedExpressionGenerator<MySQLExpression, MySQLColumn> {

    private final MySQLGlobalState state;
    private MySQLRowValue rowVal;

    public MySQLExpressionGenerator(MySQLGlobalState state) {
        this.state = state;
    }

    public MySQLExpressionGenerator setRowVal(MySQLRowValue rowVal) {
        this.rowVal = rowVal;
        return this;
    }

    private enum Actions {
        COLUMN, LITERAL, UNARY_PREFIX_OPERATION, UNARY_POSTFIX, COMPUTABLE_FUNCTION, BINARY_LOGICAL_OPERATOR,BINARY_ARITH,
        BINARY_COMPARISON_OPERATION, CAST, IN_OPERATION, BINARY_OPERATION, EXISTS, BETWEEN_OPERATOR,

        DateFunction
    }

    private enum ColumnExpressionActions {
        BINARY_COMPARISON_OPERATION, BINARY_OPERATION,BINARY_ARITH,
        COLUMN,
        COMPUTABLE_FUNCTION,
        AGGREGATE_FUNCTION,

    }
    private enum  HavingColumnExpression {
        BINARY_COMPARISON_OPERATION, BINARY_OPERATION ,
        UNARY_POSTFIX
    }
    @Override
    public MySQLExpression generateExpression(int depth) {
        if (depth >= state.getOptions().getMaxExpressionDepth()) {
            return generateLeafNode();
        }
        switch (Randomly.fromOptions(Actions.values())) {
        case COLUMN:
            return generateColumn();

        case DateFunction:
            return Randomly.fromOptions(MySQLDataExpression.DateFunction.values()).execute();
        case LITERAL:
            return generateColumn();
        case UNARY_PREFIX_OPERATION:
            MySQLExpression subExpr = generateExpression(depth + 1);
            MySQLUnaryPrefixOperator random = MySQLUnaryPrefixOperator.getRandom();
            return new MySQLUnaryPrefixOperation(subExpr, random);
        case UNARY_POSTFIX:
            return new MySQLUnaryPostfixOperation(generateExpression(depth + 1),
                    Randomly.fromOptions(MySQLUnaryPostfixOperation.UnaryPostfixOperator.values()),
                    Randomly.getBoolean());

        case COMPUTABLE_FUNCTION:
            return getComputableFunction(depth + 1);
        case BINARY_ARITH:
            return new MySQLBinaryArithOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    MySQLBinaryArithOperator.getRandom());
        case BINARY_LOGICAL_OPERATOR:
            return new MySQLBinaryLogicalOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    MySQLBinaryLogicalOperator.getRandom());
        case BINARY_COMPARISON_OPERATION:
            return new MySQLBinaryComparisonOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    BinaryComparisonOperator.getRandom());
        case CAST:
            return new MySQLCastOperation(generateExpression(depth + 1), MySQLCastOperation.CastType.getRandom());
        case IN_OPERATION:
            MySQLExpression expr = generateExpression(depth + 1);
            List<MySQLExpression> rightList = new ArrayList<>();
            for (int i = 0; i < 1 + Randomly.smallNumber(); i++) {
                rightList.add(generateExpression(depth + 1));
            }
            return new MySQLInOperation(expr, rightList, Randomly.getBoolean());


        case BINARY_OPERATION:



            return new MySQLBinaryOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    MySQLBinaryOperator.getRandom());
        case EXISTS:
            return getExists();
        case BETWEEN_OPERATOR:




            return new MySQLBetweenOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                    generateExpression(depth + 1));
        default:
            throw new AssertionError();
        }
    }


    public MySQLExpression generateFetchColumnExpression(MySQLSchema.MySQLTables targetTables) {
        switch (Randomly.fromOptions(ColumnExpressionActions.values())) {
            case BINARY_COMPARISON_OPERATION:
                return new MySQLBinaryComparisonOperation(MySQLColumnReference.create(targetTables.getColumns().get(0), null), generateExpression(2),
                        BinaryComparisonOperator.getRandom());
            case BINARY_OPERATION:
                return new MySQLBinaryOperation(MySQLColumnReference.create(targetTables.getColumns().get(0), null), generateExpression(2),
                        MySQLBinaryOperator.getRandom());
            case BINARY_ARITH:
                return new MySQLBinaryArithOperation(MySQLColumnReference.create(targetTables.getColumns().get(0), null), generateExpression(2),
                        MySQLBinaryArithOperator.getRandom());

            case AGGREGATE_FUNCTION:
                return new MySQLAggregateFunction(MySQLAggregateFunction.MySQLFunction.getRandomFunction(),generateExpression(2));
            case COMPUTABLE_FUNCTION:
                return getComputableFunction(1, MySQLColumnReference.create(targetTables.getColumns().get(0), null));
            case COLUMN:
                return generateColumn();
            default:
                throw new AssertionError();
        }
    }

    public MySQLExpression generateWhereColumnExpression(MySQLPostfixText postfixText) {
        switch (Randomly.fromOptions(HavingColumnExpression.values())) {
            case BINARY_COMPARISON_OPERATION:
                return new MySQLBinaryComparisonOperation(new MySQLManuelPredicate(postfixText.getText()), generateConstant(),
                        BinaryComparisonOperator.getRandom());
            case BINARY_OPERATION:
                return new MySQLBinaryOperation(new MySQLManuelPredicate(postfixText.getText()), generateConstant(),
                        MySQLBinaryOperator.getRandom());
            case UNARY_POSTFIX:
                return new MySQLManuelPredicate(postfixText.getText());
            default:
                throw new AssertionError();
        }
    }

    private MySQLExpression getExists() {
        if (Randomly.getBoolean()) {
            return new MySQLExists(new MySQLStringExpression("SELECT 1", MySQLConstant.createTrue()));
        } else {
            return new MySQLExists(new MySQLStringExpression("SELECT 1 wHERE FALSE", MySQLConstant.createFalse()));
        }
    }


    private MySQLExpression getComputableFunction(int depth) {
        MySQLFunction func = MySQLFunction.getRandomFunction();
        int nrArgs = func.getNrArgs();
        if (func.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        MySQLExpression[] args = new MySQLExpression[nrArgs];
        for (int i = 0; i < args.length; i++) {
            args[i] = generateExpression(depth + 1);
        }
        return new MySQLComputableFunction(func, args);
    }

    private MySQLExpression getComputableFunction(int depth,MySQLExpression exp) {
        MySQLFunction func = MySQLFunction.getRandomFunction();
        int nrArgs = func.getNrArgs();
        if (func.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        MySQLExpression[] args = new MySQLExpression[nrArgs];
        for (int i = 0; i < args.length; i++) {
            if(i == 0){
                args[i] = exp;
            }
            args[i] = generateExpression(depth + 1);
        }
        return new MySQLComputableFunction(func, args);
    }

    private enum ConstantType {
        INT, NULL, STRING, DOUBLE;

        public static ConstantType[] valuesPQS() {
            return new ConstantType[] { INT, NULL, STRING };
        }
    }

    public List<MySQLExpression> getRandomJoinClauses(List<MySQLSchema.MySQLTable> tables) {
        List<MySQLExpression> joinStatements = new ArrayList<>();
        List<MySQLJoin.JoinType> options = new ArrayList<>(Arrays.asList(MySQLJoin.JoinType.values()));
        if (Randomly.getBoolean() && tables.size() > 1) {
            int nrJoinClauses = (int) Randomly.getNotCachedInteger(0, tables.size());



            if (nrJoinClauses > 1) {
                options.remove(MySQLJoin.JoinType.NATURAL);
            }
            for (int i = 0; i < nrJoinClauses; i++) {
                MySQLSchema.MySQLTable table = Randomly.fromList(tables);
                tables.remove(table);
                setColumns(table.getColumns());
                MySQLExpression joinClause = generateExpression(2);
                MySQLJoin.JoinType selectedOption = Randomly.fromList(options);
                if (selectedOption == MySQLJoin.JoinType.NATURAL) {

                    joinClause = null;
                }
                MySQLJoin j = new MySQLJoin(table, joinClause, selectedOption);
                joinStatements.add(j);
            }

        }
        return joinStatements;
    }

    public MySQLExpression generateWindowFuc(){
        MySQLWindowFunction.MySQLFunction func = MySQLWindowFunction.MySQLFunction.getRandomFunction();

        if (func.getArgs() == 0) {
            return new MySQLWindowFunction(func,null,generateColumn());
        } else if (func.getArgs() == 1) {
            return new MySQLWindowFunction(func, generateExpression(2),generateColumn());
        }else {
            throw new AssertionError("WindowFunc args wrong");
        }
    }

    @Override
    
    public MySQLExpression generateConstant() {
        ConstantType[] values;
        if (state.usesPQS()) {
            values = ConstantType.valuesPQS();
        } else {
            values = ConstantType.values();
        }
        switch (Randomly.fromOptions(values)) {
        case INT:
            return MySQLConstant.createIntConstant((int) state.getRandomly().getInteger());
        case NULL:
            return MySQLConstant.createNullConstant();
        case STRING:
            
            String string = state.getRandomly().getString().replace("\\", "").replace("\n", "");
            return MySQLConstant.createStringConstant(string);
        case DOUBLE:
            double val = state.getRandomly().getDouble();
            return new MySQLDoubleConstant(val);
        default:
            throw new AssertionError();
        }
    }


    @Override
    public MySQLExpression generateColumn() {
        MySQLColumn c = Randomly.fromList(columns);
        MySQLConstant val;
        if (rowVal == null) {
            val = null;
        } else {
            val = rowVal.getValues().get(c);
        }
        return MySQLColumnReference.create(c, val);
    }

    @Override
    public MySQLExpression negatePredicate(MySQLExpression predicate) {
        return new MySQLUnaryPrefixOperation(predicate, MySQLUnaryPrefixOperator.NOT);
    }

    @Override
    public MySQLExpression isNull(MySQLExpression expr) {
        return new MySQLUnaryPostfixOperation(expr, MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, false);
    }

    @Override
    public List<MySQLExpression> generateOrderBys() {
        List<MySQLExpression> expressions = super.generateOrderBys();
        List<MySQLExpression> newOrderBys = new ArrayList<>();
        for (MySQLExpression expr : expressions) {
            if (Randomly.getBoolean()) {
                MySQLOrderByTerm newExpr = new MySQLOrderByTerm(expr, MySQLOrder.getRandomOrder());
                newOrderBys.add(newExpr);
            } else {
                newOrderBys.add(expr);
            }
        }
        return newOrderBys;
    }

}
