package fintechfrauds.serve.security;

import fintechfrauds.serve.config.FintechFraudsProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterService {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final FintechFraudsProperties.RateLimits config;

  public RateLimiterService(FintechFraudsProperties properties) {
    this.config = properties.getRateLimits();
  }

  public boolean allowRequest(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
    return bucket.tryConsume(1);
  }

  private Bucket createBucket(String key) {
    Refill refill = Refill.intervally(config.getRefillTokens(), Duration.ofSeconds(config.getRefillPeriodSeconds()));
    Bandwidth limit = Bandwidth.classic(config.getCapacity(), refill);
    return Bucket.builder().addLimit(limit).build();
  }
}
