package fintechfrauds.serve.security;

import static org.assertj.core.api.Assertions.assertThat;

import fintechfrauds.serve.config.FintechFraudsProperties;
import org.junit.jupiter.api.Test;

class ApiKeyServiceTest {

  @Test
  void normalizesLookupsAcrossFormats() {
    FintechFraudsProperties properties = new FintechFraudsProperties();
    properties.getSecurity().getApiKeys().put("Demo_Key", "secret-value");

    ApiKeyService service = new ApiKeyService(properties);

    assertThat(service.findSecret("demo_key")).contains("secret-value");
    assertThat(service.findSecret("demo-key")).contains("secret-value");
    assertThat(service.findSecret("DEMO-KEY")).contains("secret-value");
    assertThat(service.findSecret("missing")).isEmpty();
  }
}
