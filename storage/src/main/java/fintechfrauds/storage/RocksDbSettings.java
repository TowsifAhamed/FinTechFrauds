package fintechfrauds.storage;

public record RocksDbSettings(String path, long blockCacheBytes) {}
