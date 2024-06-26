package Sonar;

import Sonar.common.log.LoggableFactory;

public interface DatabaseProvider<G extends GlobalState<O, ?, C>, O extends DBMSSpecificOptions<?>, C extends SonarDBConnection> {

    
    Class<G> getGlobalStateClass();

    
    Class<O> getOptionClass();

    
    Reproducer<G> generateAndTestDatabase(G globalState) throws Exception;

    
    void generateAndTestDatabaseWithQueryPlanGuidance(G globalState) throws Exception;

    C createDatabase(G globalState) throws Exception;

    
    String getDBMSName();

    LoggableFactory getLoggableFactory();

    StateToReproduce getStateToReproduce(String databaseName);

}
