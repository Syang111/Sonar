package Sonar.postgres;

import Sonar.DBMSSpecificOptions;
import Sonar.OracleFactory;
import Sonar.common.oracle.CompositeTestOracle;
import Sonar.common.oracle.TestOracle;
import Sonar.postgres.PostgresOptions.PostgresOracleFactory;
import Sonar.postgres.oracle.*;
import Sonar.postgres.oracle.tlp.PostgresTLPAggregateOracle;
import Sonar.postgres.oracle.tlp.PostgresTLPHavingOracle;
import Sonar.postgres.oracle.tlp.PostgresTLPWhereOracle;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Parameters(separators = "=", commandDescription = "PostgreSQL (default port: " + PostgresOptions.DEFAULT_PORT
        + ", default host: " + PostgresOptions.DEFAULT_HOST + ")")
public class PostgresOptions implements DBMSSpecificOptions<PostgresOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5432;

    @Parameter(names = "--bulk-insert", description = "Specifies whether INSERT statements should be issued in bulk", arity = 1)
    public boolean allowBulkInsert;

    @Parameter(names = "--oracle", description = "Specifies which test oracle should be used for PostgreSQL")
    public List<PostgresOracleFactory> oracle = Arrays.asList(PostgresOracleFactory.Sonar);

    @Parameter(names = "--test-collations", description = "Specifies whether to test different collations", arity = 1)
    public boolean testCollations = true;

    @Parameter(names = "--connection-url", description = "Specifies the URL for connecting to the PostgreSQL server", arity = 1)
    public String connectionURL = String.format("postgresql://%s:%d/test", PostgresOptions.DEFAULT_HOST,
            PostgresOptions.DEFAULT_PORT);

    @Parameter(names = "--extensions", description = "Specifies a comma-separated list of extension names to be created in each test database", arity = 1)
    public String extensions = "";

    public enum PostgresOracleFactory implements OracleFactory<PostgresGlobalState> {
        NOREC {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                return new PostgresNoRECOracle(globalState);
            }
        },
        Sonar {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                return new PostgresSonarOracle(globalState);
            }
        },
        PQS {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                return new PostgresPivotedQuerySynthesisOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }
        },
        HAVING {

            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                return new PostgresTLPHavingOracle(globalState);
            }

        },
        QUERY_PARTITIONING {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                List<TestOracle<PostgresGlobalState>> oracles = new ArrayList<>();
                oracles.add(new PostgresTLPWhereOracle(globalState));
                oracles.add(new PostgresTLPHavingOracle(globalState));
                oracles.add(new PostgresTLPAggregateOracle(globalState));
                return new CompositeTestOracle<PostgresGlobalState>(oracles, globalState);
            }
        },
        CERT {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws SQLException {
                return new PostgresCERTOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }
        },
        FUZZER {
            @Override
            public TestOracle<PostgresGlobalState> create(PostgresGlobalState globalState) throws Exception {
                return new PostgresFuzzer(globalState);
            }

        };

    }

    @Override
    public List<PostgresOracleFactory> getTestOracleFactory() {
        return oracle;
    }

}
