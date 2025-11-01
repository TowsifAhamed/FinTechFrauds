package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.ScoreRequest;
import fintechfrauds.serve.scoring.FeatureStore;
import fintechfrauds.serve.scoring.Features;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScoreControllerTest {

  @Autowired private TestRestTemplate restTemplate;

  @TestConfiguration
  static class StubFeatureStoreConfig {
    @Bean
    @Primary
    FeatureStore featureStore() {
      return accountHash -> new Features(0.0, 0, false, "5999");
    }
  }

  @Test
  void scoreEndpointReturnsDecision() {
    ScoreRequest request =
        new ScoreRequest(
            "acct_demo",
            System.currentTimeMillis(),
            "gift cards",
            1_000_000L,
            "m_demo",
            "6540",
            "US");

    ResponseEntity<String> response = restTemplate.postForEntity("/v1/score", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"decision\"");
  }
}
