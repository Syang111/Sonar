package Sonar.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import com.google.auto.service.AutoService;

import Sonar.AbstractAction;
import Sonar.DatabaseProvider;
import Sonar.IgnoreMeException;
import Sonar.MainOptions;
import Sonar.Randomly;
import Sonar.SQLConnection;
import Sonar.SQLProviderAdapter;
import Sonar.StatementExecutor;
import Sonar.common.DBMSCommon;
import Sonar.common.query.ExpectedErrors;
import Sonar.common.query.SQLQueryAdapter;
import Sonar.common.query.SQLQueryProvider;
import Sonar.mysql.MySQLOptions.MySQLOracleFactory;
import Sonar.mysql.MySQLSchema.MySQLColumn;
import Sonar.mysql.MySQLSchema.MySQLTable;
import Sonar.mysql.gen.MySQLAlterTable;
import Sonar.mysql.gen.MySQLDeleteGenerator;
import Sonar.mysql.gen.MySQLDropIndex;
import Sonar.mysql.gen.MySQLInsertGenerator;
import Sonar.mysql.gen.MySQLSetGenerator;
import Sonar.mysql.gen.MySQLTableGenerator;
import Sonar.mysql.gen.MySQLTruncateTableGenerator;
import Sonar.mysql.gen.MySQLUpdateGenerator;
import Sonar.mysql.gen.admin.MySQLFlush;
import Sonar.mysql.gen.admin.MySQLReset;
import Sonar.mysql.gen.datadef.MySQLIndexGenerator;
import Sonar.mysql.gen.tblmaintenance.MySQLAnalyzeTable;
import Sonar.mysql.gen.tblmaintenance.MySQLCheckTable;
import Sonar.mysql.gen.tblmaintenance.MySQLChecksum;
import Sonar.mysql.gen.tblmaintenance.MySQLOptimize;
import Sonar.mysql.gen.tblmaintenance.MySQLRepair;

@AutoService(DatabaseProvider.class)
public class MySQLProvider extends SQLProviderAdapter<MySQLGlobalState, MySQLOptions> {

    public MySQLProvider() {
        super(MySQLGlobalState.class, MySQLOptions.class);
    }

    enum Action implements AbstractAction<MySQLGlobalState> {
        SHOW_TABLES((g) -> new SQLQueryAdapter("SHOW TABLES")),
        INSERT(MySQLInsertGenerator::insertRow),
        SET_VARIABLE(MySQLSetGenerator::set),
        REPAIR(MySQLRepair::repair),
        OPTIMIZE(MySQLOptimize::optimize),
        CHECKSUM(MySQLChecksum::checksum),
        CHECK_TABLE(MySQLCheckTable::check),
        ANALYZE_TABLE(MySQLAnalyzeTable::analyze),
        FLUSH(MySQLFlush::create), RESET(MySQLReset::create), CREATE_INDEX(MySQLIndexGenerator::create),
        ALTER_TABLE(MySQLAlterTable::create),
        TRUNCATE_TABLE(MySQLTruncateTableGenerator::generate),
        SELECT_INFO((g) -> new SQLQueryAdapter(
                "select TABLE_NAME, ENGINE from information_schema.TABLES where table_schema = '" + g.getDatabaseName()
                        + "'")),
        CREATE_TABLE((g) -> {

            String tableName = DBMSCommon.createTableName(g.getSchema().getDatabaseTables().size());
            return MySQLTableGenerator.generate(g, tableName);
        }),
        UPDATE(MySQLUpdateGenerator::create),
        DELETE(MySQLDeleteGenerator::delete),
        DROP_INDEX(MySQLDropIndex::generate);

        private final SQLQueryProvider<MySQLGlobalState> sqlQueryProvider;

        Action(SQLQueryProvider<MySQLGlobalState> sqlQueryProvider) {
            this.sqlQueryProvider = sqlQueryProvider;
        }

        @Override
        public SQLQueryAdapter getQuery(MySQLGlobalState globalState) throws Exception {
            return sqlQueryProvider.getQuery(globalState);
        }
    }

    private static int mapActions(MySQLGlobalState globalState, Action a) {
        Randomly r = globalState.getRandomly();
        int nrPerformed = 0;
        switch (a) {
        case DROP_INDEX:

            nrPerformed = r.getInteger(0, 1);
            break;
        case SHOW_TABLES:
            nrPerformed = r.getInteger(0, 0);
            break;
        case CREATE_TABLE:
            nrPerformed = r.getInteger(0, 1);
            break;
        case INSERT:

            nrPerformed = r.getInteger(1, 4);
            break;
        case REPAIR:
            nrPerformed = r.getInteger(0, 1);
            break;
        case SET_VARIABLE:
            nrPerformed = r.getInteger(0, 1);
            break;
        case CREATE_INDEX:

            nrPerformed = r.getInteger(1, 3);
            break;
        case FLUSH:
            nrPerformed = Randomly.getBooleanWithSmallProbability() ? r.getInteger(0, 1) : 0;
            break;
        case OPTIMIZE:

            nrPerformed = Randomly.getBooleanWithSmallProbability() ? r.getInteger(0, 1) : 0;
            break;
        case RESET:

            nrPerformed = globalState.getOptions().getNumberConcurrentThreads() == 1 ? r.getInteger(0, 1) : 0;
            break;
        case CHECKSUM:
        case CHECK_TABLE:
        case ANALYZE_TABLE:

            nrPerformed = r.getInteger(0, 1);
            break;
        case ALTER_TABLE:

            nrPerformed = r.getInteger(0, 1);
            break;
        case TRUNCATE_TABLE:
            nrPerformed = r.getInteger(0, 1);
            break;
        case SELECT_INFO:
            nrPerformed = r.getInteger(0, 1);
            break;
        case UPDATE:

            nrPerformed = r.getInteger(0, 1);
            break;
        case DELETE:

            nrPerformed = r.getInteger(0, 1);
            break;
        default:
            throw new AssertionError(a);
        }
        return nrPerformed;
    }

    @Override
    public void generateDatabase(MySQLGlobalState globalState) throws Exception {
        while (globalState.getSchema().getDatabaseTables().size() < Randomly.smallNumber() + 2) {
            String tableName = DBMSCommon.createTableName(globalState.getSchema().getDatabaseTables().size());
            SQLQueryAdapter createTable = MySQLTableGenerator.generate(globalState, tableName);
            globalState.executeStatement(createTable);
        }

        StatementExecutor<MySQLGlobalState, Action> se = new StatementExecutor<>(globalState, Action.values(),
                MySQLProvider::mapActions, (q) -> {
                    if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                        throw new IgnoreMeException();
                    }
                });
        se.executeStatements();

        if (globalState.getDbmsSpecificOptions().getTestOracleFactory().stream()
                .anyMatch((o) -> o == MySQLOracleFactory.CERT)) {

            ExpectedErrors errors = new ExpectedErrors();
            MySQLErrors.addExpressionErrors(errors);
            for (MySQLTable table : globalState.getSchema().getDatabaseTables()) {
                StringBuilder sb = new StringBuilder();
                sb.append("ANALYZE TABLE ");
                sb.append(table.getName());
                sb.append(" UPDATE HISTOGRAM ON ");
                String columns = table.getColumns().stream().map(MySQLColumn::getName)
                        .collect(Collectors.joining(", "));
                sb.append(columns + ";");
                globalState.executeStatement(new SQLQueryAdapter(sb.toString(), errors));
            }
        }
    }

    @Override
    public SQLConnection createDatabase(MySQLGlobalState globalState) throws SQLException {
        String username = globalState.getOptions().getUserName();
        String password = globalState.getOptions().getPassword();
        String host = globalState.getOptions().getHost();
        int port = globalState.getOptions().getPort();
        if (host == null) {
            host = MySQLOptions.DEFAULT_HOST;
        }
        if (port == MainOptions.NO_SET_PORT) {
            port = MySQLOptions.DEFAULT_PORT;
        }
        String databaseName = globalState.getDatabaseName();
        globalState.getState().logStatement("DROP DATABASE IF EXISTS " + databaseName);
        globalState.getState().logStatement("CREATE DATABASE " + databaseName);
        globalState.getState().logStatement("USE " + databaseName);
        String url = String.format("jdbc:mysql://%s:%d?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true",
                host, port);
        Connection con = DriverManager.getConnection(url, username, password);
        try (Statement s = con.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + databaseName);
        }
        try (Statement s = con.createStatement()) {
            s.execute("CREATE DATABASE " + databaseName);
        }
        try (Statement s = con.createStatement()) {
            s.execute("USE " + databaseName);
        }
        return new SQLConnection(con);
    }

    @Override
    public String getDBMSName() {
        return "mysql";
    }

    @Override
    public boolean addRowsToAllTables(MySQLGlobalState globalState) throws Exception {
        List<MySQLTable> tablesNoRow = globalState.getSchema().getDatabaseTables().stream()
                .filter(t -> t.getNrRows(globalState) == 0).collect(Collectors.toList());
        for (MySQLTable table : tablesNoRow) {
            SQLQueryAdapter queryAddRows = MySQLInsertGenerator.insertRow(globalState, table);
            globalState.executeStatement(queryAddRows);
        }
        return true;
    }

}
