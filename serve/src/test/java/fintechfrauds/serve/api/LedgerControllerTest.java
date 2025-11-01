package fintechfrauds.serve.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.ledger.LedgerService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LedgerControllerTest {

  private final LedgerService ledgerService = mock(LedgerService.class);
  private final LedgerController controller = new LedgerController(ledgerService);

  @Test
  void pendingNextReturnsNoContentWhenQueueEmpty() {
    when(ledgerService.peekPending()).thenReturn(null);

    ResponseEntity<Map<String, Object>> response = controller.pendingNext();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void pendingNextReturnsHeadOfQueue() {
    FraudReportPayload payload = samplePayload();
    LedgerService.PendingReport pending =
        new LedgerService.PendingReport("id-1", Instant.parse("2025-10-28T10:21:30Z"), payload);
    when(ledgerService.peekPending()).thenReturn(pending);

    ResponseEntity<Map<String, Object>> response = controller.pendingNext();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("id", "id-1");
    assertThat(response.getBody()).containsEntry("queuedAt", "2025-10-28T10:21:30Z");
    assertThat(response.getBody()).containsEntry("payload", payload);
  }

  private static FraudReportPayload samplePayload() {
    return new FraudReportPayload(
        "2025-10-28T10:21:30Z",
        "reporter",
        "acct",
        "merchant",
        java.util.List.of("token"),
        "5999",
        java.util.List.of("BURST"),
        "demo://evidence/123",
        null,
        null,
        null,
        null,
        null);
  }
}
