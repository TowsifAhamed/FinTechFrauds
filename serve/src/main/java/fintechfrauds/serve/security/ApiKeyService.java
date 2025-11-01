package fintechfrauds.serve.security;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

@Service
public class ApiKeyService {
  private final Map<String, String> apiSecrets;

  @Autowired
  public ApiKeyService(Environment environment) {
    this(loadSecrets(environment));
  }

  ApiKeyService(Map<String, String> secrets) {
    this.apiSecrets = normalize(secrets);
  }

  public boolean exists(String apiKey) {
    return apiSecrets.containsKey(apiKey);
  }

  public String secretFor(String apiKey) {
    return apiSecrets.get(apiKey);
  }

  private static Map<String, String> loadSecrets(Environment environment) {
    var binder = Binder.get(environment);
    Map<String, String> secrets = new LinkedHashMap<>();

    binder
        .bind("fintechfrauds.security.api-keys", Bindable.mapOf(String.class, String.class))
        .ifBound(secrets::putAll);
    binder
        .bind("fintechfrauds.security.apikeys", Bindable.mapOf(String.class, String.class))
        .ifBound(secrets::putAll);
    collectEnvironmentStyleSecrets(environment, secrets);
    return secrets;
  }

  private static void collectEnvironmentStyleSecrets(Environment environment, Map<String, String> sink) {
    if (!(environment instanceof ConfigurableEnvironment configurable)) {
      return;
    }
    for (PropertySource<?> source : configurable.getPropertySources()) {
      if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
        continue;
      }
      for (String name : enumerable.getPropertyNames()) {
        if (name == null) {
          continue;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        String keyPortion = null;
        if (upper.startsWith("FINTECHFRAUDS_SECURITY_APIKEYS_")) {
          keyPortion = name.substring("FINTECHFRAUDS_SECURITY_APIKEYS_".length());
        } else if (upper.startsWith("FINTECHFRAUDS_SECURITY_API_KEYS_")) {
          keyPortion = name.substring("FINTECHFRAUDS_SECURITY_API_KEYS_".length());
        }
        if (keyPortion == null || keyPortion.isBlank()) {
          continue;
        }
        Object value = enumerable.getProperty(name);
        if (value != null) {
          sink.put(keyPortion.toLowerCase(Locale.ROOT), value.toString());
        }
      }
    }
  }

  private static Map<String, String> normalize(Map<String, String> secrets) {
    Map<String, String> normalized = new LinkedHashMap<>();
    secrets.forEach(
        (key, value) -> {
          if (key == null || key.isBlank()) {
            return;
          }
          if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Secret for API key '" + key + "' must not be blank");
          }
          putAlias(normalized, key, value);
          putAlias(normalized, key.replace('-', '_'), value);
          putAlias(normalized, key.replace('_', '-'), value);
        });
    return Map.copyOf(normalized);
  }

  private static void putAlias(Map<String, String> normalized, String alias, String value) {
    normalized.compute(
        alias,
        (k, existing) -> {
          if (existing != null && !existing.equals(value)) {
            throw new IllegalStateException(
                "Conflicting secrets configured for API key alias '" + alias + "'");
          }
          return value;
        });
  }
}
