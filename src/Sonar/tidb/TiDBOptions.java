package Sonar.tidb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import Sonar.DBMSSpecificOptions;
import Sonar.OracleFactory;
import Sonar.common.oracle.CompositeTestOracle;
import Sonar.common.oracle.TestOracle;
import Sonar.mariadb.MariaDBProvider;
import Sonar.mariadb.oracle.MariaDBSonarOracle;
import Sonar.tidb.TiDBOptions.TiDBOracleFactory;
import Sonar.tidb.TiDBProvider.TiDBGlobalState;
import Sonar.tidb.oracle.*;

@Parameters(separators = "=", commandDescription = "TiDB (default port: " + TiDBOptions.DEFAULT_PORT
        + ", default host: " + TiDBOptions.DEFAULT_HOST + ")")
public class TiDBOptions implements DBMSSpecificOptions<TiDBOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 4000;

    @Parameter(names = { "--max-num-tables" }, description = "The maximum number of tables/views that can be created")
    public int maxNumTables = 10;

    @Parameter(names = { "--max-num-indexes" }, description = "The maximum number of indexes that can be created")
    public int maxNumIndexes = 20;

    @Parameter(names = "--oracle")
    public List<TiDBOracleFactory> oracle = Arrays.asList(TiDBOracleFactory.Sonar);

    @Parameter(names = "--enable-non-prepared-plan-cache")
    public boolean nonPreparePlanCache;

    public enum TiDBOracleFactory implements OracleFactory<TiDBGlobalState> {
        HAVING {
            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                return new TiDBTLPHavingOracle(globalState);
            }
        },
        WHERE {
            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                return new TiDBTLPWhereOracle(globalState);
            }
        },
        QUERY_PARTITIONING {
            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                List<TestOracle<TiDBGlobalState>> oracles = new ArrayList<>();
                oracles.add(new TiDBTLPWhereOracle(globalState));
                oracles.add(new TiDBTLPHavingOracle(globalState));
                return new CompositeTestOracle<TiDBGlobalState>(oracles, globalState);
            }
        },
        Sonar {

            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                return new TiDBSonarOracle(globalState);
            }

        },
        NOREC {
            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                return new TiDBNoRECOracle(globalState);
            }
        },
        CERT {
            @Override
            public TestOracle<TiDBGlobalState> create(TiDBGlobalState globalState) throws SQLException {
                return new TiDBCERTOracle(globalState);
            }

            @Override
            public boolean requiresAllTablesToContainRows() {
                return true;
            }
        };

    }

    @Override
    public List<TiDBOracleFactory> getTestOracleFactory() {
        return oracle;
    }
}
