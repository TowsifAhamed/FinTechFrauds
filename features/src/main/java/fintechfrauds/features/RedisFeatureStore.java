package fintechfrauds.features;

import fintechfrauds.core.Txn;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import redis.clients.jedis.JedisPooled;

/**
 * Minimal Redis-backed feature store using hash structures per account.
 */
public final class RedisFeatureStore implements FeatureStore {
  private final JedisPooled jedis;
  private final String prefix;

  public RedisFeatureStore(JedisPooled jedis, String prefix) {
    this.jedis = jedis;
    this.prefix = prefix.endsWith(":") ? prefix : prefix + ":";
  }

  @Override
  public void update(Txn txn) {
    String key = key(txn.accountHash());
    Map<String, String> updates = new HashMap<>();
    updates.put("last_amount", Long.toString(txn.amountCents()));
    updates.put("last_epoch", Long.toString(txn.epochMillis()));
    jedis.hset(key, updates);
    jedis.expire(key, (int) Duration.ofDays(90).getSeconds());
  }

  @Override
  public Optional<FeatureSnapshot> load(String accountHash) {
    String key = key(accountHash);
    Map<String, String> raw = jedis.hgetAll(key);
    if (raw == null || raw.isEmpty()) {
      return Optional.empty();
    }
    Map<String, Double> aggregates = new HashMap<>();
    Map<String, Instant> timestamps = new HashMap<>();
    raw.forEach((k, v) -> {
      if (k.endsWith("_epoch")) {
        timestamps.put(k, Instant.ofEpochMilli(Long.parseLong(v)));
      } else {
        aggregates.put(k, Double.parseDouble(v));
      }
    });
    return Optional.of(new FeatureSnapshot(accountHash, aggregates, timestamps));
  }

  @Override
  public void expire(String accountHash, Duration ttl) {
    jedis.expire(key(accountHash), (int) ttl.getSeconds());
  }

  private String key(String accountHash) {
    return prefix + accountHash;
  }
}
