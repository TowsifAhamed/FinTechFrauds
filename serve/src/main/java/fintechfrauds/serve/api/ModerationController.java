package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.ModerationDecision;
import fintechfrauds.serve.ledger.LedgerService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ledger")
public class ModerationController {
  private final LedgerService ledgerService;

  public ModerationController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @PostMapping("/moderate")
  public ResponseEntity<Map<String, Object>> moderate(@Valid @RequestBody ModerationDecision decision) {
    LedgerService.PendingReport head = ledgerService.peekPending();
    if (head == null) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    if (!Objects.equals(head.id(), decision.id())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("status", "MISMATCH", "expectedId", head.id()));
    }
    LedgerService.PendingReport entry = ledgerService.pollPending();
    if (entry == null || !Objects.equals(entry.id(), decision.id())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("status", "RETRY", "expectedId", head.id()));
    }
    if ("APPROVE".equalsIgnoreCase(decision.action())) {
      ledgerService.approve(entry, decision.moderator());
      return ResponseEntity.ok(
          Map.of(
              "status", "APPROVED", "id", decision.id(), "queuedAt", entry.queuedAt().toString()));
    }
    ledgerService.reject(entry);
    return ResponseEntity.ok(
        Map.of(
            "status", "REJECTED", "id", decision.id(), "queuedAt", entry.queuedAt().toString()));
  }
}
