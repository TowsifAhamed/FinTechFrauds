package fintechfrauds.serve.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fintechfrauds.serve.config.FintechFraudsProperties;
import fintechfrauds.serve.security.HmacVerifier;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LedgerFlowTest {

  private static final String API_KEY = "demo_key";
  private static final String API_SECRET = "demo_shared_secret_please_rotate";

  @Autowired private MockMvc mockMvc;
  @Autowired private HmacVerifier hmacVerifier;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private FintechFraudsProperties properties;

  private Path ledgerPath;
  private JsonSchema schema;

  @BeforeEach
  void setup() throws Exception {
    ledgerPath =
        Path.of("serve")
            .resolve(properties.getLedger().getApprovedFile())
            .toAbsolutePath()
            .normalize();
    Files.createDirectories(ledgerPath.getParent());
    Files.deleteIfExists(ledgerPath);
    ClassPathResource schemaResource = new ClassPathResource("ledger/approved-ledger.schema.json");
    try (InputStream stream = schemaResource.getInputStream()) {
      schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(stream);
    }
  }

  @AfterEach
  void cleanup() throws Exception {
    Files.deleteIfExists(ledgerPath);
  }

  @Test
  void reportModerateFlowAppendsValidatedLedger() throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "reporter", "risk_ops",
                "accountHash", "acct_masked",
                "descriptionTokensHash", "tokens_hash",
                "description", "STORED_VALUE_PROVIDER",
                "amountCents", 12500,
                "merchantHash", "merchant_masked"));

    MvcResult reportResult =
        mockMvc
            .perform(post("/v1/ledger/report").headers(authHeaders(payload)).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isAccepted())
            .andReturn();
    JsonNode reportResponse = objectMapper.readTree(reportResult.getResponse().getContentAsString());
    String reportId = reportResponse.path("id").asText();
    assertThat(reportId).isNotBlank();

    MvcResult countResult =
        mockMvc
            .perform(get("/v1/ledger/pending/count").headers(authHeaders("")))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode countNode = objectMapper.readTree(countResult.getResponse().getContentAsString());
    assertThat(countNode.path("count").asInt()).isGreaterThan(0);

    String moderationBody =
        objectMapper.writeValueAsString(
            Map.of("id", reportId, "action", "APPROVE", "moderator", "unit-test"));
    mockMvc
        .perform(post("/v1/ledger/moderate").headers(authHeaders(moderationBody)).contentType(MediaType.APPLICATION_JSON).content(moderationBody))
        .andExpect(status().isOk());

    assertThat(Files.exists(ledgerPath)).isTrue();
    String line = Files.readString(ledgerPath, StandardCharsets.UTF_8).trim();
    assertThat(line).isNotEmpty();

    ObjectNode tree = (ObjectNode) objectMapper.readTree(line);
    assertThat(schema.validate(tree)).isEmpty();
    assertThat(tree.path("status").asText()).isEqualTo("APPROVED");
    assertThat(tree.path("prevHash").isNull()).isTrue();
    String prevHash = tree.path("prevHash").isNull() ? null : tree.path("prevHash").asText(null);
    tree.remove("hash");
    ObjectMapper canonicalMapper = objectMapper.copy();
    canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    String canonical = canonicalMapper.writeValueAsString(tree);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] expected =
        digest.digest(((prevHash == null ? "" : prevHash) + canonical).getBytes(StandardCharsets.UTF_8));
    assertThat(objectMapper.readTree(line).path("hash").asText())
        .isEqualTo(Base64.getEncoder().encodeToString(expected));
  }

  @Test
  void duplicateReportsForSameDayAreRejected() throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            Map.of(
                "reporter", "risk_ops",
                "accountHash", "acct_masked",
                "descriptionTokensHash", "tokens_hash",
                "description", "STORED_VALUE_PROVIDER",
                "amountCents", 12500,
                "merchantHash", "merchant_masked"));

    MvcResult reportResult =
        mockMvc
            .perform(
                post("/v1/ledger/report")
                    .headers(authHeaders(payload))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isAccepted())
            .andReturn();

    String reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).path("id").asText();

    String moderationBody =
        objectMapper.writeValueAsString(
            Map.of("id", reportId, "action", "APPROVE", "moderator", "unit-test"));
    mockMvc
        .perform(
            post("/v1/ledger/moderate")
                .headers(authHeaders(moderationBody))
                .contentType(MediaType.APPLICATION_JSON)
                .content(moderationBody))
        .andExpect(status().isOk());

    assertThat(Files.exists(ledgerPath)).isTrue();
    long count;
    try (java.util.stream.Stream<String> lines = Files.lines(ledgerPath, StandardCharsets.UTF_8)) {
      count = lines.filter(line -> !line.isBlank()).count();
    }
    assertThat(count).isEqualTo(1);

    mockMvc
        .perform(
            post("/v1/ledger/report")
                .headers(authHeaders(payload))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }

  private org.springframework.http.HttpHeaders authHeaders(String body) {
    String timestamp = Instant.now().toString();
    String nonce = UUID.randomUUID().toString();
    String bodyHash = hmacVerifier.sha256Hex(body);
    String canonical = hmacVerifier.canonicalRequest(timestamp, nonce, bodyHash);
    String signature = hmacVerifier.sign(API_SECRET, canonical);

    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Api-Key", API_KEY);
    headers.add("X-Timestamp", timestamp);
    headers.add("X-Nonce", nonce);
    headers.add("X-Signature", signature);
    headers.add("X-Idempotency-Key", UUID.randomUUID().toString());
    return headers;
  }
}
