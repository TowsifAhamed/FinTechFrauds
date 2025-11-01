package fintechfrauds.serve.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final long capacity;
  private final long refillTokens;
  private final Duration refillPeriod;

  public RateLimiterService(Environment environment) {
    this.capacity =
        Long.parseLong(environment.getProperty("fintechfrauds.rateLimits.capacity", "60"));
    this.refillTokens =
        Long.parseLong(environment.getProperty("fintechfrauds.rateLimits.refillTokens", "60"));
    long periodSeconds =
        Long.parseLong(environment.getProperty("fintechfrauds.rateLimits.refillPeriodSeconds", "60"));
    this.refillPeriod = Duration.ofSeconds(periodSeconds);
  }

  public boolean tryConsume(String key) {
    return buckets.computeIfAbsent(key, this::newBucket).tryConsume(1);
  }

  private Bucket newBucket(String ignoredKey) {
    Refill refill = Refill.greedy(refillTokens, refillPeriod);
    Bandwidth limit = Bandwidth.classic(capacity, refill);
    return Bucket4j.builder().addLimit(limit).build();
  }
}
