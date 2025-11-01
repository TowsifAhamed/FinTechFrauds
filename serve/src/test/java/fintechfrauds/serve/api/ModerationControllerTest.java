package fintechfrauds.serve.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.api.dto.ModerationDecision;
import fintechfrauds.serve.ledger.LedgerService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ModerationControllerTest {

  private final LedgerService ledgerService = mock(LedgerService.class);
  private final ModerationController controller = new ModerationController(ledgerService);

  @Test
  void returnsNoContentWhenQueueEmpty() {
    when(ledgerService.peekPending()).thenReturn(null);

    ResponseEntity<Map<String, Object>> response =
        controller.moderate(new ModerationDecision("id", "APPROVE", "moderator"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(ledgerService, never()).pollPending();
  }

  @Test
  void returnsConflictWhenIdDiffers() {
    FraudReportPayload payload = samplePayload();
    LedgerService.PendingReport pending = pending("expected", payload);
    when(ledgerService.peekPending()).thenReturn(pending);

    ResponseEntity<Map<String, Object>> response =
        controller.moderate(new ModerationDecision("other", "APPROVE", "moderator"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).containsEntry("status", "MISMATCH");
    verify(ledgerService, never()).pollPending();
  }

  @Test
  void approvesWhenIdsMatch() {
    FraudReportPayload payload = samplePayload();
    LedgerService.PendingReport pending = pending("id-1", payload);
    when(ledgerService.peekPending()).thenReturn(pending);
    when(ledgerService.pollPending()).thenReturn(pending);

    ResponseEntity<Map<String, Object>> response =
        controller.moderate(new ModerationDecision("id-1", "APPROVE", "moderator"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "APPROVED");
    assertThat(response.getBody()).containsEntry("queuedAt", pending.queuedAt().toString());
    verify(ledgerService).pollPending();
    verify(ledgerService).approve(pending, "moderator");
  }

  @Test
  void rejectsWhenIdsMatch() {
    FraudReportPayload payload = samplePayload();
    LedgerService.PendingReport pending = pending("id-1", payload);
    when(ledgerService.peekPending()).thenReturn(pending);
    when(ledgerService.pollPending()).thenReturn(pending);

    ResponseEntity<Map<String, Object>> response =
        controller.moderate(new ModerationDecision("id-1", "REJECT", "moderator"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "REJECTED");
    assertThat(response.getBody()).containsEntry("queuedAt", pending.queuedAt().toString());
    verify(ledgerService).pollPending();
    verify(ledgerService).reject(pending);
  }

  private static LedgerService.PendingReport pending(String id, FraudReportPayload payload) {
    return new LedgerService.PendingReport(id, Instant.parse("2025-10-28T10:21:30Z"), payload);
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
