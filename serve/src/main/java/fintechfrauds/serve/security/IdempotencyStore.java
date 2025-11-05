package fintechfrauds.serve.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;

@Component
public class IdempotencyStore {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyStore.class);
  private final JedisPooled jedis;
  private final Map<String, Instant> inMemoryCache = new ConcurrentHashMap<>();

  public IdempotencyStore(JedisPooled jedis) {
    this.jedis = jedis;
  }

  public boolean register(String key, Duration ttl) {
    cleanupExpired();
    try {
      Long result = jedis.setnx(key, "1");
      if (result != null && result == 1L) {
        jedis.pexpire(key, ttl.toMillis());
        return true;
      }
      return false;
    } catch (JedisException e) {
      log.debug("idempotency_store_fallback", e);
      Instant expiresAt = Instant.now().plus(ttl);
      return inMemoryCache.putIfAbsent(key, expiresAt) == null;
    }
  }

  private void cleanupExpired() {
    Instant now = Instant.now();
    Iterator<Map.Entry<String, Instant>> iterator = inMemoryCache.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Instant> entry = iterator.next();
      if (entry.getValue().isBefore(now)) {
        iterator.remove();
      }
    }
  }
}
