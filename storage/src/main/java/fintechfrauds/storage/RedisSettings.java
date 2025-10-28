package fintechfrauds.storage;

public record RedisSettings(String host, int port, String namespace) {
  public String key(String suffix) {
    return namespace + ":" + suffix;
  }
}
