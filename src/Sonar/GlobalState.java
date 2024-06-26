package Sonar;

import Sonar.common.query.Query;
import Sonar.common.query.SonarResultSet;
import Sonar.common.schema.AbstractSchema;
import Sonar.common.schema.AbstractTable;

public abstract class GlobalState<O extends DBMSSpecificOptions<?>, S extends AbstractSchema<?, ?>, C extends SonarDBConnection> {

    protected C databaseConnection;
    private Randomly r;
    private MainOptions options;
    private O dbmsSpecificOptions;
    private S schema;
    private Main.StateLogger logger;
    private StateToReproduce state;
    private Main.QueryManager<C> manager;
    private String databaseName;

    private int columns = 0;

    public int getColumns() {
        return columns;
    }

    public void addColumns(int i){
        columns += i;
    }

    public void setConnection(C con) {
        this.databaseConnection = con;
    }

    public C getConnection() {
        return databaseConnection;
    }

    @SuppressWarnings("unchecked")
    public void setDbmsSpecificOptions(Object dbmsSpecificOptions) {
        this.dbmsSpecificOptions = (O) dbmsSpecificOptions;
    }

    public O getDbmsSpecificOptions() {
        return dbmsSpecificOptions;
    }

    public void setRandomly(Randomly r) {
        this.r = r;
    }

    public Randomly getRandomly() {
        return r;
    }

    public MainOptions getOptions() {
        return options;
    }

    public void setMainOptions(MainOptions options) {
        this.options = options;
    }

    public void setStateLogger(Main.StateLogger logger) {
        this.logger = logger;
    }

    public Main.StateLogger getLogger() {
        return logger;
    }

    public void setState(StateToReproduce state) {
        this.state = state;
    }

    public StateToReproduce getState() {
        return state;
    }

    public Main.QueryManager<C> getManager() {
        return manager;
    }

    public void setManager(Main.QueryManager<C> manager) {
        this.manager = manager;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    private ExecutionTimer executePrologue(Query<?> q) throws Exception {
        boolean logExecutionTime = getOptions().logExecutionTime();
        ExecutionTimer timer = null;
        if (logExecutionTime) {
            timer = new ExecutionTimer().start();
        }
        if (getOptions().printAllStatements()) {
            System.out.println(q.getLogString());
        }
        return timer;
    }

    protected abstract void executeEpilogue(Query<?> q, boolean success, ExecutionTimer timer) throws Exception;

    public boolean executeStatement(Query<C> q, String... fills) throws Exception {
        ExecutionTimer timer = executePrologue(q);
        boolean success = manager.execute(q, fills);
        executeEpilogue(q, success, timer);
        return success;
    }

    public SonarResultSet executeStatementAndGet(Query<C> q, String... fills) throws Exception {
        ExecutionTimer timer = executePrologue(q);
        SonarResultSet result = manager.executeAndGet(q, fills);
        boolean success = result != null;
        if (success) {
            result.registerEpilogue(() -> {
                try {
                    executeEpilogue(q, success, timer);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
        return result;
    }

    public S getSchema() {
        if (schema == null) {
            try {
                updateSchema();
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        }
        return schema;
    }

    protected void setSchema(S schema) {
        this.schema = schema;
    }

    public void updateSchema() throws Exception {
        setSchema(readSchema());
        for (AbstractTable<?, ?, ?> table : schema.getDatabaseTables()) {
            table.recomputeCount();
        }
    }

    protected abstract S readSchema() throws Exception;

}
