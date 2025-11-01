package fintechfrauds.serve.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {
  private final ApiKeyService apiKeyService;
  private final RateLimiterService rateLimiterService;
  private final IdempotencyStore idempotencyStore;
  private final long allowedSkewSeconds;

  public ApiAuthFilter(
      ApiKeyService apiKeyService,
      RateLimiterService rateLimiterService,
      IdempotencyStore idempotencyStore,
      Environment environment) {
    this.apiKeyService = apiKeyService;
    this.rateLimiterService = rateLimiterService;
    this.idempotencyStore = idempotencyStore;
    this.allowedSkewSeconds =
        Long.parseLong(environment.getProperty("fintechfrauds.security.requestTimeSkewSeconds", "300"));
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String uri = request.getRequestURI();
    return uri.startsWith("/actuator")
        || uri.equals("/")
        || uri.startsWith("/v1/score")
        || uri.startsWith("/v3/api-docs")
        || uri.startsWith("/swagger-ui")
        || uri.equals("/swagger-ui.html");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String apiKey = request.getHeader("X-Api-Key");
    String signature = request.getHeader("X-Signature");
    String timestamp = request.getHeader("X-Timestamp");
    String nonce = request.getHeader("X-Nonce");
    String idempotencyKey = request.getHeader("X-Idempotency-Key");

    if (apiKey == null || signature == null || timestamp == null || nonce == null) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing auth headers");
      return;
    }

    if (!apiKeyService.exists(apiKey)) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Unknown API key");
      return;
    }

    String rateLimitKey = apiKey + ":" + request.getRemoteAddr();
    if (!rateLimiterService.tryConsume(rateLimitKey)) {
      reject(response, 429, "Rate limit exceeded");
      return;
    }

    byte[] body = request.getInputStream().readAllBytes();
    String secret = apiKeyService.secretFor(apiKey);
    if (!HmacVerifier.verify(signature, secret, timestamp, nonce, body, allowedSkewSeconds)) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature or timestamp");
      return;
    }

    if ("POST".equalsIgnoreCase(request.getMethod())) {
      if (idempotencyKey == null || idempotencyKey.isBlank()) {
        reject(response, HttpServletResponse.SC_BAD_REQUEST, "Missing X-Idempotency-Key");
        return;
      }
      boolean stored = idempotencyStore.putIfAbsent("idem:" + idempotencyKey, 86_400);
      if (!stored) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":\"DUPLICATE\"}");
        return;
      }
    }

    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
    filterChain.doFilter(wrappedRequest, response);
  }

  private void reject(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
