package Sonar.mariadb;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import Sonar.DBMSSpecificOptions;
import Sonar.OracleFactory;
import Sonar.common.oracle.TestOracle;
import Sonar.mariadb.MariaDBOptions.MariaDBOracleFactory;
import Sonar.mariadb.MariaDBProvider.MariaDBGlobalState;
import Sonar.mariadb.oracle.MariaDBNoRECOracle;
import Sonar.mariadb.oracle.MariaDBSonarOracle;

@Parameters(separators = "=", commandDescription = "MariaDB (default port: " + MariaDBOptions.DEFAULT_PORT
        + ", default host: " + MariaDBOptions.DEFAULT_HOST + ")")
public class MariaDBOptions implements DBMSSpecificOptions<MariaDBOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;

    @Parameter(names = "--oracle")
    public List<MariaDBOracleFactory> oracles = Arrays.asList(MariaDBOracleFactory.Sonar);

    public enum MariaDBOracleFactory implements OracleFactory<MariaDBGlobalState> {

        NOREC {

            @Override
            public TestOracle<MariaDBGlobalState> create(MariaDBGlobalState globalState) throws SQLException {
                return new MariaDBNoRECOracle(globalState);
            }

        },
        Sonar {

            @Override
            public TestOracle<MariaDBGlobalState> create(MariaDBGlobalState globalState) throws SQLException {
                return new MariaDBSonarOracle(globalState);
            }

        }
    }

    @Override
    public List<MariaDBOracleFactory> getTestOracleFactory() {
        return oracles;
    }

}
