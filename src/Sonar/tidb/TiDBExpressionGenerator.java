package Sonar.tidb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Sonar.IgnoreMeException;
import Sonar.Randomly;
import Sonar.common.gen.UntypedExpressionGenerator;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.TiDBSchema.TiDBColumn;
import Sonar.tidb.TiDBSchema.TiDBDataType;
import Sonar.tidb.ast.*;
import Sonar.tidb.ast.TiDBAggregate.TiDBAggregateFunction;
import Sonar.tidb.ast.TiDBBinaryBitOperation.TiDBBinaryBitOperator;
import Sonar.tidb.ast.TiDBBinaryComparisonOperation.TiDBComparisonOperator;
import Sonar.tidb.ast.TiDBBinaryLogicalOperation.TiDBBinaryLogicalOperator;
import Sonar.tidb.ast.TiDBFunctionCall.TiDBFunction;
import Sonar.tidb.ast.TiDBRegexOperation.TiDBRegexOperator;
import Sonar.tidb.ast.TiDBUnaryPostfixOperation.TiDBUnaryPostfixOperator;
import Sonar.tidb.ast.TiDBUnaryPrefixOperation.TiDBUnaryPrefixOperator;

public class TiDBExpressionGenerator extends UntypedExpressionGenerator<TiDBExpression, TiDBColumn> {

    private final TiDBGlobalState globalState;

    public TiDBExpressionGenerator(TiDBGlobalState globalState) {
        this.globalState = globalState;
    }

    private enum Gen {
        UNARY_PREFIX,
        UNARY_POSTFIX,
        CONSTANT,
        COLUMN,
        COMPARISON, REGEX, FUNCTION, BINARY_LOGICAL, BINARY_BIT, CAST, DEFAULT, CASE,
        DateFunction

    }

    private enum ColumnExpressionActions {
        BINARY_COMPARISON_OPERATION, BINARY_ARITH,
        COLUMN,
        COMPUTABLE_FUNCTION,
    }

    private enum WhereColumnExpression {
        BINARY_COMPARISON_OPERATION, BINARY_ARITH
    }

    public TiDBExpression generateFetchColumnExpression(TiDBSchema.TiDBColumn c) {
        switch (Randomly.fromOptions(ColumnExpressionActions.values())) {
            case BINARY_COMPARISON_OPERATION:
                return new TiDBBinaryComparisonOperation(new TiDBColumnReference(c), generateExpression(2),
                        TiDBBinaryComparisonOperation.TiDBComparisonOperator.getRandom());
            case BINARY_ARITH:
                return new TiDBBinaryArithmeticOperation(new TiDBColumnReference(c), generateExpression(2),
                        TiDBBinaryArithmeticOperation.TiDBBinaryArithmeticOperator.getRandom());
            case COMPUTABLE_FUNCTION:
                return getComputableFunction(2, new TiDBColumnReference(c));
            case COLUMN:
                return generateColumn();
            default:
                throw new AssertionError();
        }
    }

    public TiDBExpression generateWhereColumnExpression(TiDBPostfixText postfixText) {
        switch (Randomly.fromOptions(WhereColumnExpression.values())) {
            case BINARY_COMPARISON_OPERATION:
                return new TiDBBinaryComparisonOperation(new TiDBManuelPredicate(postfixText.getText()), generateConstant(),
                        TiDBBinaryComparisonOperation.TiDBComparisonOperator.getRandom());
            case BINARY_ARITH:
                return new TiDBBinaryArithmeticOperation(new TiDBManuelPredicate(postfixText.getText()), generateConstant(),
                        TiDBBinaryArithmeticOperation.TiDBBinaryArithmeticOperator.getRandom());
            default:
                throw new AssertionError();
        }
    }

    private TiDBExpression getComputableFunction(int depth, TiDBExpression exp) {
        TiDBFunction func = TiDBFunction.getRandom();

        int nrArgs = func.getNrArgs();

        List<TiDBExpression> args = new ArrayList<>();
        for (int i = 0; i < nrArgs; i++) {
            if (i == 0) {
                args.add(exp);
            } else {
                args.add(generateExpression(depth + 1));
            }
        }
        return new TiDBFunctionCall(func, args);

    }

    @Override
    protected TiDBExpression generateExpression(int depth) {
        if (depth >= globalState.getOptions().getMaxExpressionDepth() || Randomly.getBoolean()) {
            return generateLeafNode();
        }
        if (allowAggregates && Randomly.getBoolean()) {
            allowAggregates = false;
            TiDBAggregateFunction func = TiDBAggregateFunction.getRandom();
            List<TiDBExpression> args = generateExpressions(func.getNrArgs());
            return new TiDBAggregate(args, func);
        }
        switch (Randomly.fromOptions(Gen.values())) {
            case DEFAULT:
                if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                    throw new IgnoreMeException();
                }
                TiDBColumn column = Randomly.fromList(columns);
                if (column.hasDefault()) {
                    return new TiDBFunctionCall(TiDBFunction.DEFAULT, Arrays.asList(new TiDBColumnReference(column)));
                }
                throw new IgnoreMeException();
            case UNARY_POSTFIX:
                return new TiDBUnaryPostfixOperation(generateExpression(depth + 1), TiDBUnaryPostfixOperator.getRandom());
            case UNARY_PREFIX:
                TiDBUnaryPrefixOperator rand = TiDBUnaryPrefixOperator.getRandom();
                return new TiDBUnaryPrefixOperation(generateExpression(depth + 1), rand);
            case COLUMN:
                return generateColumn();
            case CONSTANT:
                return generateConstant();
            case COMPARISON:
                return new TiDBBinaryComparisonOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                        TiDBComparisonOperator.getRandom());
            case REGEX:
                return new TiDBRegexOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                        TiDBRegexOperator.getRandom());
            case FUNCTION:
                TiDBFunction func = TiDBFunction.getRandom();
                return new TiDBFunctionCall(func, generateExpressions(func.getNrArgs(), depth));
            case BINARY_BIT:
                return new TiDBBinaryBitOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                        TiDBBinaryBitOperator.getRandom());
            case BINARY_LOGICAL:
                return new TiDBBinaryLogicalOperation(generateExpression(depth + 1), generateExpression(depth + 1),
                        TiDBBinaryLogicalOperator.getRandom());
            case CAST:
                return new TiDBCastOperation(generateExpression(depth + 1), Randomly.fromOptions("BINARY",
                        "CHAR", "DATE", "DATETIME", "TIME",
                        "DECIMAL", "SIGNED", "UNSIGNED" ));
            case CASE:
                int nr = Randomly.fromOptions(1, 2);
                return new TiDBCase(generateExpression(depth + 1), generateExpressions(nr, depth + 1),
                        generateExpressions(nr, depth + 1), generateExpression(depth + 1));
            case DateFunction:
                return Randomly.fromOptions(TiDBDateExpression.DateFunction.values()).execute();
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected TiDBExpression generateColumn() {
        TiDBColumn column = Randomly.fromList(columns);
        return new TiDBColumnReference(column);
    }

    @Override
    public TiDBExpression generateConstant() {
        TiDBDataType type = TiDBDataType.getRandom();
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return TiDBConstant.createNullConstant();
        }
        switch (type) {
            case INT:
                return TiDBConstant.createIntConstant(globalState.getRandomly().getInteger());
            case BLOB:
            case TEXT:
                return TiDBConstant.createStringConstant(globalState.getRandomly().getString());
            case BOOL:
                return TiDBConstant.createBooleanConstant(Randomly.getBoolean());
            case FLOATING:
                return TiDBConstant.createFloatConstant(globalState.getRandomly().getDouble());
            case CHAR:
                return TiDBConstant.createStringConstant(globalState.getRandomly().getChar());
            case DECIMAL:
            case NUMERIC:
                return TiDBConstant.createIntConstant(globalState.getRandomly().getInteger());
            default:
                throw new AssertionError();
        }
    }

    @Override
    public List<TiDBExpression> generateOrderBys() {
        List<TiDBExpression> expressions = super.generateOrderBys();
        List<TiDBExpression> newExpressions = new ArrayList<>();
        for (TiDBExpression expr : expressions) {
            TiDBExpression newExpr = expr;
            if (Randomly.getBoolean()) {
                newExpr = new TiDBOrderingTerm(expr, Randomly.getBoolean());
            }
            newExpressions.add(newExpr);
        }
        return newExpressions;
    }

    @Override
    public TiDBExpression negatePredicate(TiDBExpression predicate) {
        return new TiDBUnaryPrefixOperation(predicate, TiDBUnaryPrefixOperator.NOT);
    }

    @Override
    public TiDBExpression isNull(TiDBExpression expr) {
        return new TiDBUnaryPostfixOperation(expr, TiDBUnaryPostfixOperator.IS_NULL);
    }

    public TiDBExpression generateConstant(TiDBDataType type) {
        if (Randomly.getBooleanWithRatherLowProbability()) {
            return TiDBConstant.createNullConstant();
        }
        switch (type) {
            case INT:
                return TiDBConstant.createIntConstant(globalState.getRandomly().getInteger());
            case BLOB:
            case TEXT:
                return TiDBConstant.createStringConstant(globalState.getRandomly().getString());
            case BOOL:
                return TiDBConstant.createBooleanConstant(Randomly.getBoolean());
            case FLOATING:
                return TiDBConstant.createFloatConstant(globalState.getRandomly().getDouble());
            case CHAR:
                return TiDBConstant.createStringConstant(globalState.getRandomly().getChar());
            case DECIMAL:
            case NUMERIC:
                return TiDBConstant.createIntConstant(globalState.getRandomly().getInteger());
            default:
                throw new AssertionError();
        }
    }

}
