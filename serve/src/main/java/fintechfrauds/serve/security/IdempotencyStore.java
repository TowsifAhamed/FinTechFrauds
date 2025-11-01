package fintechfrauds.serve.security;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

@Service
public class IdempotencyStore {
  private final JedisPool jedisPool;

  public IdempotencyStore(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  /**
   * @return {@code true} if the key was stored for the first time, {@code false} if it already exists.
   */
  public boolean putIfAbsent(String key, long ttlSeconds) {
    try (Jedis jedis = jedisPool.getResource()) {
      SetParams params = SetParams.setParams().nx().ex((int) ttlSeconds);
      String result = jedis.set(key, "1", params);
      return "OK".equalsIgnoreCase(result);
    }
  }
}
