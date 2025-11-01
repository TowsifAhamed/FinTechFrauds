package fintechfrauds.serve.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ApiKeyServiceTest {

  @Test
  void bindsHyphenatedProperties() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("fintechfrauds.security.api-keys.demo", "secret-value");

    ApiKeyService service = new ApiKeyService(environment);

    assertThat(service.exists("demo")).isTrue();
    assertThat(service.secretFor("demo")).isEqualTo("secret-value");
  }

  @Test
  void bindsCamelCaseProperties() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("fintechfrauds.security.apikeys.alt", "another-secret");

    ApiKeyService service = new ApiKeyService(environment);

    assertThat(service.exists("alt")).isTrue();
    assertThat(service.secretFor("alt")).isEqualTo("another-secret");
  }

  @Test
  void bindsEnvironmentStyleUppercaseProperties() {
    MockEnvironment environment = new MockEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new org.springframework.core.env.SystemEnvironmentPropertySource(
                "test-env",
                java.util.Map.of("FINTECHFRAUDS_SECURITY_APIKEYS_DEMO_KEY", "env-secret")));

    ApiKeyService service = new ApiKeyService(environment);

    assertThat(service.exists("demo_key")).isTrue();
    assertThat(service.secretFor("demo_key")).isEqualTo("env-secret");
  }

  @Test
  void bindsEnvironmentStyleWithSeparatedKeysPrefix() {
    MockEnvironment environment = new MockEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new org.springframework.core.env.SystemEnvironmentPropertySource(
                "test-env-alt",
                java.util.Map.of("FINTECHFRAUDS_SECURITY_API_KEYS_ALT_KEY", "alt-secret")));

    ApiKeyService service = new ApiKeyService(environment);

    assertThat(service.exists("alt_key")).isTrue();
    assertThat(service.secretFor("alt_key")).isEqualTo("alt-secret");
  }

  @Test
  void normalizesHyphenAndUnderscoreVariants() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("fintechfrauds.security.apikeys.demo-key", "secret-value");

    ApiKeyService service = new ApiKeyService(environment);

    assertThat(service.exists("demo-key")).isTrue();
    assertThat(service.secretFor("demo-key")).isEqualTo("secret-value");
    assertThat(service.exists("demo_key")).isTrue();
    assertThat(service.secretFor("demo_key")).isEqualTo("secret-value");
  }

  @Test
  void rejectsConflictingAliases() {
    Map<String, String> secrets = new LinkedHashMap<>();
    secrets.put("demo-key", "secret-one");
    secrets.put("demo_key", "secret-two");

    assertThatThrownBy(() -> new ApiKeyService(secrets))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("demo_key");
  }

  @Test
  void rejectsBlankSecrets() {
    Map<String, String> secrets = Map.of("demo-key", " ");

    assertThatThrownBy(() -> new ApiKeyService(secrets))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("demo-key");
  }
}
