package fintechfrauds.serve.security;

import fintechfrauds.serve.config.FintechFraudsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiAuthFilter.class);
  private static final Set<String> BYPASS_PREFIXES =
      new HashSet<>(Arrays.asList("/actuator", "/swagger-ui", "/v3/api-docs"));

  private final ApiKeyService apiKeyService;
  private final HmacVerifier hmacVerifier;
  private final IdempotencyStore idempotencyStore;
  private final RateLimiterService rateLimiterService;
  private final long skewSeconds;
  private final Environment environment;

  public ApiAuthFilter(
      ApiKeyService apiKeyService,
      HmacVerifier hmacVerifier,
      IdempotencyStore idempotencyStore,
      RateLimiterService rateLimiterService,
      FintechFraudsProperties properties,
      Environment environment) {
    this.apiKeyService = apiKeyService;
    this.hmacVerifier = hmacVerifier;
    this.idempotencyStore = idempotencyStore;
    this.rateLimiterService = rateLimiterService;
    this.skewSeconds = properties.getSecurity().getRequestTimeSkewSeconds();
    this.environment = environment;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = resolvePath(request);
    for (String prefix : BYPASS_PREFIXES) {
      if (path.startsWith(prefix)) {
        return true;
      }
    }
    if (!isProd() && "/v1/score".equals(path)) {
      return true;
    }
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
    String apiKey = request.getHeader("X-Api-Key");
    String timestamp = request.getHeader("X-Timestamp");
    String nonce = request.getHeader("X-Nonce");
    String signature = request.getHeader("X-Signature");
    String path = resolvePath(request);

    if (StringUtils.isBlank(apiKey)
        || StringUtils.isBlank(timestamp)
        || StringUtils.isBlank(nonce)
        || StringUtils.isBlank(signature)) {
      log.warn("auth_headers_missing path={}", path);
      reject(response, HttpStatus.UNAUTHORIZED, "Missing authentication headers");
      return;
    }

    Optional<String> secret = apiKeyService.findSecret(apiKey);
    if (secret.isEmpty()) {
      log.warn("auth_invalid_api_key key={} path={}", apiKey, path);
      reject(response, HttpStatus.FORBIDDEN, "Invalid API key");
      return;
    }

    if (!hmacVerifier.verifyTimestamp(timestamp, skewSeconds)) {
      log.warn("auth_timestamp_skew path={} timestamp={} skew={}s", path, timestamp, skewSeconds);
      reject(response, HttpStatus.UNAUTHORIZED, "Timestamp outside allowed skew");
      return;
    }

    if (!rateLimiterService.allowRequest(apiKey + ":" + request.getRemoteAddr())) {
      log.warn("auth_rate_limit_exceeded key={} ip={} path={}", apiKey, request.getRemoteAddr(), path);
      reject(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
      return;
    }

    String body = cachedRequest.getCachedBody();
    String canonical =
        hmacVerifier.canonicalRequest(timestamp, nonce, hmacVerifier.sha256Hex(body));
    String expectedSignature = hmacVerifier.sign(secret.get(), canonical);
    byte[] expectedBytes = java.util.Base64.getDecoder().decode(expectedSignature);
    byte[] providedBytes;
    try {
      providedBytes = java.util.Base64.getDecoder().decode(signature);
    } catch (IllegalArgumentException ex) {
      log.warn("auth_signature_decoding_failed key={} path={}", apiKey, path);
      reject(response, HttpStatus.UNAUTHORIZED, "Signature decoding failed");
      return;
    }
    if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
      log.warn("auth_signature_mismatch key={} path={}", apiKey, path);
      reject(response, HttpStatus.UNAUTHORIZED, "Signature mismatch");
      return;
    }

    if ("POST".equalsIgnoreCase(request.getMethod())) {
      String idempotencyKey = request.getHeader("X-Idempotency-Key");
      if (StringUtils.isBlank(idempotencyKey)) {
        log.warn("auth_missing_idempotency path={}", path);
        reject(response, HttpStatus.BAD_REQUEST, "Missing X-Idempotency-Key header");
        return;
      }
      boolean firstSeen =
          idempotencyStore.register(
              apiKey + ":" + idempotencyKey, Duration.ofHours(isProd() ? 24 : 1));
      if (!firstSeen) {
        log.warn("auth_duplicate_request key={} idemKey={} path={}", apiKey, idempotencyKey, path);
        reject(response, HttpStatus.CONFLICT, "Duplicate request");
        return;
      }
    }

    filterChain.doFilter(cachedRequest, response);
  }

  private String resolvePath(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    if (StringUtils.isNotBlank(servletPath)) {
      return servletPath;
    }
    String requestUri = request.getRequestURI();
    return requestUri == null ? "" : requestUri;
  }

  private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }

  private boolean isProd() {
    return Arrays.asList(environment.getActiveProfiles()).contains("prod");
  }
}
