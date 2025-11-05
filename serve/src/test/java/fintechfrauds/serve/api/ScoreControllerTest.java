package fintechfrauds.serve.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ScoreControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void scoreAllFixtures() throws Exception {
    ClassPathResource fixtures =
        new ClassPathResource("testdata/fintechfrauds_testcases.jsonl");
    Map<String, String> scenarioDecisions = new HashMap<>();
    Map<String, Integer> scenarioReasonCounts = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(fixtures.getInputStream(), StandardCharsets.UTF_8))) {
      reader.lines()
          .forEach(
              line -> {
                try {
                  MvcResult result =
                      mockMvc
                          .perform(
                              post("/v1/score").contentType(MediaType.APPLICATION_JSON).content(line))
                          .andExpect(status().isOk())
                          .andReturn();
                  JsonNode response =
                      objectMapper.readTree(result.getResponse().getContentAsString());
                  assertThat(response.has("risk")).isTrue();
                  assertThat(response.has("decision")).isTrue();
                  assertThat(response.has("reasons")).isTrue();
                  JsonNode request = objectMapper.readTree(line);
                  String scenarioId = request.path("scenarioId").asText();
                  scenarioDecisions.put(scenarioId, response.path("decision").asText());
                  scenarioReasonCounts.put(scenarioId, response.path("reasons").size());
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
    }

    Set<String> reviewExpected = Set.of("C2", "C3", "D1", "E1");
    for (String scenario : reviewExpected) {
      assertThat(scenarioDecisions)
          .as("Scenario %s should not be APPROVE", scenario)
          .containsKey(scenario);
      assertThat(scenarioDecisions.get(scenario)).isNotEqualTo("APPROVE");
    }

    Set<String> approveExpected = Set.of("A1", "A3", "F1", "F2", "G1", "G2");
    for (String scenario : approveExpected) {
      assertThat(scenarioDecisions)
          .as("Scenario %s should not be DECLINE", scenario)
          .containsKey(scenario);
      assertThat(scenarioDecisions.get(scenario)).isNotEqualTo("DECLINE");
    }

    scenarioDecisions.forEach(
        (scenario, decision) -> {
          if (!"APPROVE".equals(decision)) {
            assertThat(scenarioReasonCounts.getOrDefault(scenario, 0))
                .as("Scenario %s should return at least one reason when %s", scenario, decision)
                .isGreaterThan(0);
          }
        });
  }
}
