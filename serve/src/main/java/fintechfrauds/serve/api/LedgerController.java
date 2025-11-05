package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.ledger.DuplicateReportException;
import fintechfrauds.serve.ledger.LedgerService;
import fintechfrauds.serve.ledger.LedgerService.PendingReport;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ledger")
public class LedgerController {

  private final LedgerService ledgerService;

  public LedgerController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @PostMapping("/report")
  public ResponseEntity<Map<String, Object>> report(@Valid @RequestBody FraudReportPayload payload) {
    try {
      PendingReport pending = ledgerService.enqueue(payload);
      return ResponseEntity.accepted()
          .body(Map.of("id", pending.id(), "queuedAt", Instant.now().toString()));
    } catch (DuplicateReportException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", ex.getMessage()));
    }
  }

  @GetMapping("/pending/count")
  public Map<String, Object> pendingCount() {
    return Map.of("count", ledgerService.pendingCount());
  }

  @GetMapping("/pending/next")
  public ResponseEntity<?> pendingNext() {
    Optional<PendingReport> pending = ledgerService.peekNext();
    if (pending.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    PendingReport report = pending.get();
    return ResponseEntity.ok(
        Map.of(
            "id",
            report.id(),
            "receivedAt",
            report.receivedAt().toString(),
            "payload",
            report.payload()));
  }
}
