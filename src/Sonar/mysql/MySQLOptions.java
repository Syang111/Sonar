package Sonar.mysql;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import Sonar.DBMSSpecificOptions;
import Sonar.OracleFactory;
import Sonar.common.oracle.TestOracle;
import Sonar.mysql.MySQLOptions.MySQLOracleFactory;
import Sonar.mysql.oracle.*;

@Parameters(separators = "=", commandDescription = "MySQL (default port: " + MySQLOptions.DEFAULT_PORT
        + ", default host: " + MySQLOptions.DEFAULT_HOST + ")")
public class MySQLOptions implements DBMSSpecificOptions<MySQLOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;

    @Parameter(names = "--oracle")
    public List<MySQLOracleFactory> oracles = Arrays.asList(MySQLOracleFactory.Sonar);

    public enum MySQLOracleFactory implements OracleFactory<MySQLGlobalState> {

        TLP_WHERE {

            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws SQLException {
                return new MySQLTLPWhereOracle(globalState);
            }

        },
        PQS {

            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws SQLException {
                return new MySQLPivotedQuerySynthesisOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }

        },
        CERT {
            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws SQLException {
                return new MySQLCERTOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }
        },
        FUZZER {
            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws Exception {
                return new MySQLFuzzer(globalState);
            }

        },
        NoREC {
            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws Exception {
                return new MySQLNoRECOracle(globalState);
            }
        },
        Sonar {
            @Override
            public TestOracle<MySQLGlobalState> create(MySQLGlobalState globalState) throws Exception {
                return new MySQLSonarOracle(globalState);
            }
        };
    }

    @Override
    public List<MySQLOracleFactory> getTestOracleFactory() {
        return oracles;
    }

}
