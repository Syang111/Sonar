package Sonar.mysql.ast;

import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import Sonar.Randomly;
import Sonar.mysql.MySQLSchema.MySQLDataType;
import Sonar.mysql.ast.MySQLCastOperation.CastType;

public class MySQLComputableFunction implements MySQLExpression {

    private final MySQLFunction func;
    private final MySQLExpression[] args;

    public MySQLComputableFunction(MySQLFunction func, MySQLExpression... args) {
        this.func = func;
        this.args = args.clone();
    }

    public MySQLFunction getFunction() {
        return func;
    }

    public MySQLExpression[] getArguments() {
        return args.clone();
    }

    public enum MySQLFunction {

        ABS(1, "ABS") {
            @Override
            public MySQLConstant apply(MySQLConstant[] args, MySQLExpression[] origArgs) {
                if (args[0].isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                MySQLConstant intVal = args[0].castAs(CastType.SIGNED);
                return MySQLConstant.createIntConstant(Math.abs(intVal.getInt()));
            }
        },
        
        BIT_COUNT(1, "BIT_COUNT") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                } else {
                    long val = arg.castAs(CastType.SIGNED).getInt();
                    return MySQLConstant.createIntConstant(Long.bitCount(val));
                }
            }

        },
        ACOS(1, "ACOS") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        ASIN(1, "ASIN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        ATAN(1, "ATAN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                    return null;
            }
        },
        ATAN2(1, "ATAN2") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        CEILING(1, "CEILING") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        COS(1, "COS") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        COT(1, "COT") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        CRC32(1, "CRC32") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        DEG(1, "DEGREES") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        EXP(1, "EXP") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        FLOOR(1, "FLOOR") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        LN(1, "LN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        LOG(1, "LOG") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        LOG2(1, "LOG2") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        LOG10(1, "LOG10") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        ROUND(1, "ROUND") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        MOD(2, "MOD") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        SIN(1, "SIN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        SQRT(1, "SQRT") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        TAN(1, "TAN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }
        },
        STRCMP(2, "STRCMP") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;
            }

        },



















        COALESCE(2, "COALESCE") {
            @Override
            public MySQLConstant apply(MySQLConstant[] args, MySQLExpression... origArgs) {
                MySQLConstant result = MySQLConstant.createNullConstant();
                for (MySQLConstant arg : args) {
                    if (!arg.isNull()) {
                        result = MySQLConstant.createStringConstant(arg.castAsString());
                        break;
                    }
                }
                return castToMostGeneralType(result, origArgs);
            }

            @Override
            public boolean isVariadic() {
                return true;
            }

        },
        


















        














        LEAST(2, "LEAST", true) {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return aggregate(evaluatedArgs, (min, cur) -> cur.isLessThan(min).asBooleanNotNull() ? cur : min);
            }

        },
        GREATEST(2, "GREATEST", true) {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return aggregate(evaluatedArgs, (max, cur) -> cur.isLessThan(max).asBooleanNotNull() ? max : cur);
            }
        };

        private String functionName;
        final int nrArgs;
        private final boolean variadic;

        private static MySQLConstant aggregate(MySQLConstant[] evaluatedArgs, BinaryOperator<MySQLConstant> op) {
            boolean containsNull = Stream.of(evaluatedArgs).anyMatch(arg -> arg.isNull());
            if (containsNull) {
                return MySQLConstant.createNullConstant();
            }
            MySQLConstant least = evaluatedArgs[1];
            for (MySQLConstant arg : evaluatedArgs) {
                MySQLConstant left = castToMostGeneralType(least, evaluatedArgs);
                MySQLConstant right = castToMostGeneralType(arg, evaluatedArgs);
                least = op.apply(right, left);
            }
            return castToMostGeneralType(least, evaluatedArgs);
        }

        MySQLFunction(int nrArgs, String functionName) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = false;
        }

        MySQLFunction(int nrArgs, String functionName, boolean variadic) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = variadic;
        }

        
        public int getNrArgs() {
            return nrArgs;
        }

        public abstract MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args);

        public static MySQLFunction getRandomFunction() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String toString() {
            return functionName;
        }

        public boolean isVariadic() {
            return variadic;
        }

        public String getName() {
            return functionName;
        }
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant[] constants = new MySQLConstant[args.length];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = args[i].getExpectedValue();
            if (constants[i].getExpectedValue() == null) {
                return null;
            }
        }
        return func.apply(constants, args);
    }

    public static MySQLConstant castToMostGeneralType(MySQLConstant cons, MySQLExpression... typeExpressions) {
        if (cons.isNull()) {
            return cons;
        }
        MySQLDataType type = getMostGeneralType(typeExpressions);
        switch (type) {
            case INT:
                if (cons.isInt()) {
                    return cons;
                } else {
                    return MySQLConstant.createIntConstant(cons.castAs(CastType.SIGNED).getInt());
                }
            case VARCHAR:
                return MySQLConstant.createStringConstant(cons.castAsString());
            default:
                throw new AssertionError(type);
        }
    }

    public static MySQLDataType getMostGeneralType(MySQLExpression... expressions) {
        MySQLDataType type = null;
        for (MySQLExpression expr : expressions) {
            MySQLDataType exprType;
            if (expr instanceof MySQLColumnReference) {
                exprType = ((MySQLColumnReference) expr).getColumn().getType();
            } else {
                exprType = expr.getExpectedValue().getType();
            }
            if (type == null) {
                type = exprType;
            } else if (exprType == MySQLDataType.VARCHAR) {
                type = MySQLDataType.VARCHAR;
            }

        }
        return type;
    }

}
