package fintechfrauds.serve.security;

import fintechfrauds.serve.config.FintechFraudsProperties;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

  private final Map<String, String> apiKeys;

  public ApiKeyService(FintechFraudsProperties properties) {
    Map<String, String> configured = properties.getSecurity().getApiKeys();
    Map<String, String> normalized = new HashMap<>();
    if (configured != null) {
      configured.forEach(
          (key, secret) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret)) {
              normalized.put(normalize(key), secret);
            }
          });
    }
    this.apiKeys = Map.copyOf(normalized);
  }

  public Optional<String> findSecret(String apiKey) {
    if (StringUtils.isBlank(apiKey)) {
      return Optional.empty();
    }
    String trimmed = apiKey.trim();
    String normalizedKey = normalize(trimmed);
    String secret = apiKeys.get(normalizedKey);
    return Optional.ofNullable(secret);
  }

  private String normalize(String key) {
    return key
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace('-', '_');
  }
}
