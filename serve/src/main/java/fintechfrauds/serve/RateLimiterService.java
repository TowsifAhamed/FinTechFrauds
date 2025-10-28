package fintechfrauds.serve;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory leaky bucket limiter for demonstration purposes.
 */
public final class RateLimiterService {
  private static final int MAX_REQUESTS = 100;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public void check(String key) {
    Window window = windows.computeIfAbsent(key, k -> new Window());
    synchronized (window) {
      Instant now = Instant.now();
      if (Duration.between(window.startedAt, now).compareTo(WINDOW) > 0) {
        window.startedAt = now;
        window.count = 0;
      }
      if (window.count >= MAX_REQUESTS) {
        throw new IllegalStateException("rate limit exceeded");
      }
      window.count++;
    }
  }

  private static final class Window {
    private Instant startedAt = Instant.EPOCH;
    private int count = 0;
  }
}
