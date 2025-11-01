package fintechfrauds.serve.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAuthFilterTest {

  @Test
  void optionsRequestsBypassAuthentication() throws ServletException, IOException {
    ApiAuthFilter filter =
        new ApiAuthFilter(
            new ApiKeyService(Map.of("demo", "secret")),
            new RateLimiterService(new MockEnvironment()),
            new NoopIdempotencyStore(),
            new MockEnvironment());

    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/v1/ledger/report");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private static final class NoopIdempotencyStore extends IdempotencyStore {
    NoopIdempotencyStore() {
      super(null);
    }

    @Override
    public boolean putIfAbsent(String key, long ttlSeconds) {
      return true;
    }
  }
}
