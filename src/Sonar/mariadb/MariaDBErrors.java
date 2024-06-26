package Sonar.mariadb;

import Sonar.common.query.ExpectedErrors;

public final class MariaDBErrors {

    private MariaDBErrors() {
    }

    public static void addInsertErrors(ExpectedErrors errors) {
        errors.add("Out of range");
        errors.add("Duplicate entry");
        errors.add("cannot be null");
        errors.add("Incorrect integer value");
        errors.add("Data truncated for column");
        errors.add("doesn't have a default value");
        errors.add("The value specified for generated column");
        errors.add("Incorrect double value");
        errors.add("Incorrect string value");
    }

}
