package Sonar;

import Sonar.common.oracle.TestOracle;

public interface OracleFactory<G extends GlobalState<?, ?, ?>> {

    TestOracle<G> create(G globalState) throws Exception;


    default boolean requiresAllTablesToContainRows() {
        return false;
    }

}
