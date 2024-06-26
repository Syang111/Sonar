package Sonar;

import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.common.query.SonarResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class ComparatorHelpercopy {

    private ComparatorHelpercopy() {
    }

    public static boolean isEqualDouble(String first, String second) {
        try {
            double val = Double.parseDouble(first);
            double secVal = Double.parseDouble(second);
            return equals(val, secVal);
        } catch (Exception e) {
            return false;
        }
    }

    static boolean equals(double a, double b) {
        if (a == b) {
            return true;
        }


        return Math.abs(a - b) < 0.001 * Math.max(Math.abs(a), Math.abs(b)) + 0.001;
    }


    public static List<String> getResultSetFirstColumnAsString(String queryString, ExpectedErrors errors,
            SQLGlobalState<?, ?> state) throws SQLException {










        boolean canonicalizeString = state.getOptions().canonicalizeSqlString();
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors, false, canonicalizeString);
        List<String> resultSet = new ArrayList<>();
        SonarResultSet result = null;
        try {
            result = q.executeAndGet(state);
            if (result == null) {
                throw new IgnoreMeException();
            }
            while (result.next()) {
                String resultTemp = result.getString(1);
                if (resultTemp != null) {
                    resultTemp = resultTemp.replaceAll("[\\.]0+$", "");

                }
                resultSet.add(resultTemp);
            }
        } catch (Exception e) {
            if (e instanceof IgnoreMeException) {
                throw e;
            }

            if (e.getMessage() == null) {
                throw new AssertionError(queryString, e);
            }
            if (errors.errorIsExpected(e.getMessage())) {
                throw new IgnoreMeException();
            }
            throw new AssertionError(queryString, e);
        } finally {
            if (result != null && !result.isClosed()) {
                result.close();
            }
        }
        return resultSet;
    }

    public static void assumeResultSetsAreEqual(List<String> resultSet, List<String> secondResultSet,
            String originalQueryString, List<String> combinedString, SQLGlobalState<?, ?> state) {
        if (resultSet.size() != secondResultSet.size()) {
            String queryFormatString = "-- %s;" + System.lineSeparator() + "-- cardinality: %d"
                    + System.lineSeparator();
            String firstQueryString = String.format(queryFormatString, originalQueryString, resultSet.size());
            String combinedQueryString = String.join(";", combinedString);
            String secondQueryString = String.format(queryFormatString, combinedQueryString, secondResultSet.size());
            state.getState().getLocalState()
                    .log(String.format("%s" + System.lineSeparator() + "%s", firstQueryString, secondQueryString));
            String assertionMessage = String.format(
                    "The size of the result sets mismatch (%d and %d)!" + System.lineSeparator()
                            + "First query: \"%s\", whose cardinality is: %d" + System.lineSeparator()
                            + "Second query:\"%s\", whose cardinality is: %d",
                    resultSet.size(), secondResultSet.size(), originalQueryString, resultSet.size(),
                    combinedQueryString, secondResultSet.size());
            throw new AssertionError(assertionMessage);
        }

        Set<String> firstHashSet = new HashSet<>(resultSet);
        Set<String> secondHashSet = new HashSet<>(secondResultSet);

        boolean validateResultSizeOnly = state.getOptions().validateResultSizeOnly();
        if (!validateResultSizeOnly && !firstHashSet.equals(secondHashSet)) {
            Set<String> firstResultSetMisses = new HashSet<>(firstHashSet);
            firstResultSetMisses.removeAll(secondHashSet);
            Set<String> secondResultSetMisses = new HashSet<>(secondHashSet);
            secondResultSetMisses.removeAll(firstHashSet);

            String queryFormatString = "-- Query: \"%s\"; It misses: \"%s\"";
            String firstQueryString = String.format(queryFormatString, originalQueryString, firstResultSetMisses);
            String secondQueryString = String.format(queryFormatString, String.join(";", combinedString),
                    secondResultSetMisses);

            state.getState().getLocalState()
                    .log(String.format("%s" + System.lineSeparator() + "%s", firstQueryString, secondQueryString));
            String assertionMessage = String.format("The content of the result sets mismatch!" + System.lineSeparator()
                    + "First query : \"%s\"" + System.lineSeparator() + "Second query: \"%s\"", originalQueryString,
                    secondQueryString);
            throw new AssertionError(assertionMessage);
        }
    }

    public static void assumeResultSetsAreEqual(List<String> resultSet, List<String> secondResultSet,
                                                String originalQueryString, String changeQueryString, SQLGlobalState<?, ?> state) {
        if (resultSet.size() != secondResultSet.size()) {
            String queryFormatString = "-- %s;" + System.lineSeparator() + "-- cardinality: %d"
                    + System.lineSeparator();
            String firstQueryString = String.format(queryFormatString, originalQueryString, resultSet.size());
            String secondQueryString = String.format(queryFormatString, changeQueryString, secondResultSet.size());
            state.getState().getLocalState()
                    .log(String.format("%s" + System.lineSeparator() + "%s", firstQueryString, secondQueryString));
            String assertionMessage = String.format(
                    "The size of the result sets mismatch (%d and %d)!" + System.lineSeparator()
                            + "First query: \"%s\", whose cardinality is: %d" + System.lineSeparator()
                            + "Second query:\"%s\", whose cardinality is: %d",
                    resultSet.size(), secondResultSet.size(), originalQueryString, resultSet.size(),
                    changeQueryString, secondResultSet.size());
            throw new AssertionError(assertionMessage);
        }

        Set<String> firstHashSet = new HashSet<>(resultSet);
        Set<String> secondHashSet = new HashSet<>(secondResultSet);

        boolean validateResultSizeOnly = state.getOptions().validateResultSizeOnly();
        if (!validateResultSizeOnly && !firstHashSet.equals(secondHashSet)) {
            Set<String> firstResultSetMisses = new HashSet<>(firstHashSet);
            firstResultSetMisses.removeAll(secondHashSet);
            Set<String> secondResultSetMisses = new HashSet<>(secondHashSet);
            secondResultSetMisses.removeAll(firstHashSet);

            String queryFormatString = "-- Query: \"%s\"; It misses: \"%s\"";
            String firstQueryString = String.format(queryFormatString, originalQueryString, firstResultSetMisses);
            String secondQueryString = String.format(queryFormatString, changeQueryString, secondResultSetMisses);

            state.getState().getLocalState()
                    .log(String.format("%s" + System.lineSeparator() + "%s", firstQueryString, secondQueryString));
            String assertionMessage = String.format("The content of the result sets mismatch!" + System.lineSeparator()
                            + "First query : \"%s\"" + System.lineSeparator() + "Second query: \"%s\"", originalQueryString,
                    secondQueryString);
            throw new AssertionError(assertionMessage);
        }
    }

    public static void assumeResultSetsAreEqual(List<String> resultSet, List<String> secondResultSet,
            String originalQueryString, List<String> combinedString, SQLGlobalState<?, ?> state,
            UnaryOperator<String> canonicalizationRule) {


        List<String> canonicalizedResultSet = resultSet.stream().map(canonicalizationRule).collect(Collectors.toList());
        List<String> canonicalizedSecondResultSet = secondResultSet.stream().map(canonicalizationRule)
                .collect(Collectors.toList());
        assumeResultSetsAreEqual(canonicalizedResultSet, canonicalizedSecondResultSet, originalQueryString,
                combinedString, state);
    }

    public static List<String> getCombinedResultSet(String firstQueryString, String secondQueryString,
            String thirdQueryString, List<String> combinedString, boolean asUnion, SQLGlobalState<?, ?> state,
            ExpectedErrors errors) throws SQLException {
        List<String> secondResultSet;
        if (asUnion) {
            String unionString = firstQueryString + " UNION ALL " + secondQueryString + " UNION ALL "
                    + thirdQueryString;
            combinedString.add(unionString);
            secondResultSet = getResultSetFirstColumnAsString(unionString, errors, state);
        } else {
            secondResultSet = new ArrayList<>();
            secondResultSet.addAll(getResultSetFirstColumnAsString(firstQueryString, errors, state));
            secondResultSet.addAll(getResultSetFirstColumnAsString(secondQueryString, errors, state));
            secondResultSet.addAll(getResultSetFirstColumnAsString(thirdQueryString, errors, state));
            combinedString.add(firstQueryString);
            combinedString.add(secondQueryString);
            combinedString.add(thirdQueryString);
        }
        return secondResultSet;
    }

    public static List<String> getCombinedResultSetNoDuplicates(String firstQueryString, String secondQueryString,
            String thirdQueryString, List<String> combinedString, boolean asUnion, SQLGlobalState<?, ?> state,
            ExpectedErrors errors) throws SQLException {
        String unionString;
        if (asUnion) {
            unionString = firstQueryString + " UNION " + secondQueryString + " UNION " + thirdQueryString;
        } else {
            unionString = "SELECT DISTINCT * FROM (" + firstQueryString + " UNION ALL " + secondQueryString
                    + " UNION ALL " + thirdQueryString + ")";
        }
        List<String> secondResultSet;
        combinedString.add(unionString);
        secondResultSet = getResultSetFirstColumnAsString(unionString, errors, state);
        return secondResultSet;
    }

    public static String canonicalizeResultValue(String value) {
        if (value == null) {
            return value;
        }

        switch (value) {
        case "-0.0":
            return "0.0";
        case "-0":
            return "0";
        default:
        }

        return value;
    }

}
