package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.ledger.LedgerService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
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
    String id = UUID.randomUUID().toString();
    Instant queuedAt = Instant.now();
    ledgerService.enqueue(id, queuedAt, payload);
    return ResponseEntity.accepted()
        .body(
            Map.of(
                "status",
                "PENDING",
                "id",
                id,
                "queued",
                Boolean.TRUE,
                "queuedAt",
                queuedAt.toString()));
  }

  @GetMapping("/pending/count")
  public Map<String, Object> pendingCount() {
    return Map.of("pending", ledgerService.pendingSize());
  }

  @GetMapping("/pending/next")
  public ResponseEntity<Map<String, Object>> pendingNext() {
    LedgerService.PendingReport pending = ledgerService.peekPending();
    if (pending == null) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    return ResponseEntity.ok(
        Map.of(
            "id",
            pending.id(),
            "queuedAt",
            pending.queuedAt().toString(),
            "payload",
            pending.payload()));
  }
}
