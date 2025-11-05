package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;

@Component
public class RedisFeatureStore implements FeatureStore {

  private static final Logger log = LoggerFactory.getLogger(RedisFeatureStore.class);
  private final JedisPooled jedis;
  private final java.util.concurrent.ConcurrentHashMap<String, FallbackStats> fallbackState =
      new java.util.concurrent.ConcurrentHashMap<>();

  public RedisFeatureStore(JedisPooled jedis) {
    this.jedis = jedis;
  }

  @Override
  public FeatureVector loadFeatures(ScoreRequest request) {
    String accountHash = request.getAccountHash();
    String merchantHash = request.getMerchantHash();
    String key = String.format("fs:acct:%s", accountHash);
    try {
      Map<String, String> map = jedis.hgetAll(key);
      if (map == null || map.isEmpty()) {
        return fallbackVector(request);
      }
      double amountZ = parseDouble(map.getOrDefault("amountZ", "0"));
      int windowCount = parseInt(map.getOrDefault("window15mCount", "0"));
      int firstTimeMerchant = parseInt(map.getOrDefault("firstTimeMerchant", merchantHash == null ? "0" : "1"));
      String mcc = map.getOrDefault("mcc", "UNKNOWN");
      updateFallback(request);
      return new FeatureVector(amountZ, windowCount, firstTimeMerchant, mcc);
    } catch (JedisException e) {
      log.warn("redis_feature_load_failed", e);
      return fallbackVector(request);
    }
  }

  private FeatureVector fallbackVector(ScoreRequest request) {
    return fallbackState
        .computeIfAbsent(request.getAccountHash(), k -> new FallbackStats())
        .record(request);
  }

  private void updateFallback(ScoreRequest request) {
    fallbackState
        .computeIfAbsent(request.getAccountHash(), k -> new FallbackStats())
        .record(request);
  }

  private double parseDouble(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ex) {
      return 0.0d;
    }
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private static class FallbackStats {
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    private final List<Long> windowEvents = new ArrayList<>();
    private final Set<String> seenMerchants = new HashSet<>();
    private long count = 0;
    private double mean = 0.0d;
    private double m2 = 0.0d;

    synchronized FeatureVector record(ScoreRequest request) {
      long timestamp =
          request.getEpochMillis() != null ? request.getEpochMillis() : System.currentTimeMillis();
      int insertPos = Collections.binarySearch(windowEvents, timestamp);
      if (insertPos < 0) {
        insertPos = -(insertPos + 1);
      }
      windowEvents.add(insertPos, timestamp);
      long cutoff = timestamp - WINDOW_MILLIS;
      int idx = 0;
      while (idx < windowEvents.size() && windowEvents.get(idx) < cutoff) {
        windowEvents.remove(idx);
      }
      int windowCount = 0;
      for (long eventTimestamp : windowEvents) {
        if (eventTimestamp > timestamp) {
          break;
        }
        if (eventTimestamp >= cutoff) {
          windowCount++;
        }
      }

      String merchantHash = request.getMerchantHash();
      int firstTimeMerchant = 0;
      if (merchantHash != null) {
        if (!seenMerchants.contains(merchantHash)) {
          firstTimeMerchant = 1;
          seenMerchants.add(merchantHash);
        }
      }

      double amount = request.getAmountCents() != null ? request.getAmountCents() : 0.0d;
      count++;
      double delta = amount - mean;
      mean += delta / count;
      double delta2 = amount - mean;
      m2 += delta * delta2;
      double variance = count > 1 ? m2 / (count - 1) : 0.0d;
      double stddev = variance > 0 ? Math.sqrt(variance) : 0.0d;
      double z = stddev > 0 ? (amount - mean) / stddev : 0.0d;

      String mcc = request.getMcc() != null ? request.getMcc() : "UNKNOWN";
      return new FeatureVector(z, windowCount, firstTimeMerchant, mcc);
    }
  }
}
