package Sonar.common.oracle;

import Sonar.GlobalState;
import Sonar.Reproducer;

public interface TestOracle<G extends GlobalState<?, ?, ?>> {

    void check() throws Exception;

    default Reproducer<G> getLastReproducer() {
        return null;
    }

    default String getLastQueryString() {
        throw new AssertionError("Not supported!");
    }
}
