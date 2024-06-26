package Sonar.postgres.ast;

import Sonar.postgres.PostgresSchema.PostgresDataType;

public interface PostgresExpression {

    default PostgresDataType getExpressionType() {
        return null;
    }

    default PostgresConstant getExpectedValue() {
        return null;
    }
}
