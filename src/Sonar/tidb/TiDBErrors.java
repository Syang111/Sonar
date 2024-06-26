package Sonar.tidb;

import Sonar.common.query.ExpectedErrors;

public final class TiDBErrors {

    private TiDBErrors() {
    }

    public static void addExpressionErrors(ExpectedErrors errors) {
        errors.add("DECIMAL value is out of range");
        errors.add("error parsing regexp");
        errors.add("BIGINT UNSIGNED value is out of range");
        errors.add("Data truncation: Truncated incorrect time value");
        errors.add("Data truncation: Incorrect time value");
        errors.add("Data truncation: Incorrect datetime value");
        errors.add("overflows double");
        errors.add("overflows bigint");
        errors.add("strconv.ParseFloat: parsing");
        errors.add("in 'order clause'");


        errors.add("BIGINT value is out of range");
        errors.add("doesn't have a default value");
        errors.add("is not valid for CHARACTER SET");
        errors.add("DOUBLE value is out of range");

        errors.add("Data truncation: %s value is out of range in '%s'");
        errors.add("Truncated incorrect FLOAT value");
        errors.add("Bad Number");


        errors.add("error parsing regexp");
        errors.add("from regexp");
        errors.add("Empty pattern is invalid");
        errors.add("Invalid regexp pattern");



        errors.add("Data truncation");
        errors.add("the incremental analyze feature has already been removed");
        errors.add("For float(M,D), double(M,D) or decimal(M,D)");
        errors.add("Result of space() was larger");



        errors.add("Unknown column");
        errors.add("Can\'t find column");
        errors.add("Cannot convert");

        if (TiDBBugs.bug35677) {
            errors.add("for function inet_aton");
        }
        if (TiDBBugs.bug35522) {
            errors.add("ERROR 1054 (42S22)");
        }
        if (TiDBBugs.bug35652) {
            errors.add("from binary to utf8");
        }
        if (TiDBBugs.bug38295) {
            errors.add("assertion failed");
        }
        if (TiDBBugs.bug44747) {
            errors.add("index out of range");
        }
    }

    public static void addExpressionHavingErrors(ExpectedErrors errors) {
        errors.add("is not in GROUP BY clause and contains nonaggregated column");
        errors.add("Unknown column");
    }

    public static void addInsertErrors(ExpectedErrors errors) {
        errors.add("Duplicate entry");
        errors.add("cannot be null");
        errors.add("doesn't have a default value");
        errors.add("Out of range value");
        errors.add("Incorrect double value");
        errors.add("Incorrect float value");
        errors.add("Incorrect int value");
        errors.add("Incorrect tinyint value");
        errors.add("Data truncation");
        errors.add("Bad Number");
        errors.add("The value specified for generated column");
        errors.add("incorrect utf8 value");
        errors.add("Data truncation: %s value is out of range in '%s'");
        errors.add("Incorrect smallint value");
        errors.add("Incorrect bigint value");
        errors.add("Incorrect decimal value");
        errors.add("error parsing regexp");
        errors.add("is not valid for CHARACTER SET");
    }

}
