package Sonar;

public interface SonarDBConnection extends AutoCloseable {

    String getDatabaseVersion() throws Exception;
}
