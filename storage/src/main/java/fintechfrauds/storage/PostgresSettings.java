package fintechfrauds.storage;

public record PostgresSettings(String host, int port, String database, String user) {
  public String jdbcUrl() {
    return "jdbc:postgresql://" + host + ":" + port + "/" + database;
  }
}
