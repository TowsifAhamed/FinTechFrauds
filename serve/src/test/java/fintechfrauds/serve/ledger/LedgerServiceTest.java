package fintechfrauds.serve.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fintechfrauds.serve.api.dto.FraudReportPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LedgerServiceTest {

  @Test
  void peekDoesNotRemoveEntry() throws Exception {
    Path tempDir = Files.createTempDirectory("ledger-test");
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty(
        "fintechfrauds.ledger.approvedFile",
        tempDir.resolve("approved-ledger.jsonl").toString());

    LedgerService ledgerService = new LedgerService(environment);
    FraudReportPayload payload = samplePayload();
    ledgerService.enqueue("id-1", Instant.parse("2025-10-28T10:21:30Z"), payload);
    ledgerService.enqueue("id-2", Instant.parse("2025-10-28T11:21:30Z"), payload);

    assertThat(ledgerService.pendingSize()).isEqualTo(2);
    LedgerService.PendingReport peeked = ledgerService.peekPending();
    assertThat(peeked).isNotNull();
    assertThat(peeked.id()).isEqualTo("id-1");
    assertThat(peeked.queuedAt()).isEqualTo(Instant.parse("2025-10-28T10:21:30Z"));
    assertThat(ledgerService.pendingSize()).isEqualTo(2);

    assertThat(ledgerService.pollPending().id()).isEqualTo("id-1");
    assertThat(ledgerService.pendingSize()).isEqualTo(1);
  }

  @Test
  void approvePersistsQueuedMetadata() throws Exception {
    Path tempDir = Files.createTempDirectory("ledger-test");
    MockEnvironment environment = new MockEnvironment();
    Path file = tempDir.resolve("approved-ledger.jsonl");
    environment.setProperty("fintechfrauds.ledger.approvedFile", file.toString());

    LedgerService ledgerService = new LedgerService(environment);
    FraudReportPayload payload = samplePayload();
    Instant queuedAt = Instant.parse("2025-10-28T10:21:30Z");
    ledgerService.enqueue("id-3", queuedAt, payload);

    LedgerService.PendingReport report = ledgerService.pollPending();
    ledgerService.approve(report, "moderator");

    List<String> lines = Files.readAllLines(file);
    assertThat(lines).isNotEmpty();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(lines.get(lines.size() - 1));
    assertThat(node.get("id").asText()).isEqualTo("id-3");
    assertThat(node.get("queuedAt").asText()).isEqualTo("2025-10-28T10:21:30Z");
    assertThat(node.get("moderator").asText()).isEqualTo("moderator");
    assertThat(node.get("status").asText()).isEqualTo("APPROVED");
  }

  private static FraudReportPayload samplePayload() {
    return new FraudReportPayload(
        "2025-10-28T10:21:30Z",
        "reporter",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
