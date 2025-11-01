package fintechfrauds.serve.scoring;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
public class RedisFeatureStore implements FeatureStore {
  private final JedisPool jedisPool;

  public RedisFeatureStore(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public Features loadFeatures(String accountHash) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = "fs:acct:" + accountHash;
      String amountZ = jedis.hget(key, "amountZ");
      String window = jedis.hget(key, "window15mCount");
      String firstMerchant = jedis.hget(key, "firstTimeMerchant");
      String mcc = jedis.hget(key, "mcc");

      double amountZValue = amountZ != null ? Double.parseDouble(amountZ) : 0.0d;
      int windowValue = window != null ? Integer.parseInt(window) : 0;
      boolean firstMerchantValue = "1".equals(firstMerchant) || Boolean.parseBoolean(firstMerchant);
      return new Features(amountZValue, windowValue, firstMerchantValue, mcc);
    }
  }
}
