package fintechfrauds.serve.config;

import fintechfrauds.ledger.IdempotencyStore;
import fintechfrauds.ledger.LedgerQueue;
import fintechfrauds.ledger.SignatureVerifier;
import fintechfrauds.serve.RateLimiterService;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerConfig {
  @Bean
  public RateLimiterService rateLimiterService() {
    return new RateLimiterService();
  }

  @Bean
  public SignatureVerifier signatureVerifier() {
    return new SignatureVerifier("change-me".getBytes(StandardCharsets.UTF_8));
  }

  @Bean
  public LedgerQueue ledgerQueue() {
    return payload -> System.out.println("queued ledger payload " + payload.id());
  }

  @Bean
  public IdempotencyStore idempotencyStore() {
    return new InMemoryIdempotencyStore();
  }

  static final class InMemoryIdempotencyStore implements IdempotencyStore {
    private final Set<String> seen = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    @Override
    public void ensureNew(String key) {
      if (!seen.add(key)) {
        throw new IllegalArgumentException("duplicate idempotency key");
      }
    }
  }
}
