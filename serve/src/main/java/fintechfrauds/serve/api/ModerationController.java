package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.ModerationDecision;
import fintechfrauds.serve.ledger.DuplicateReportException;
import fintechfrauds.serve.ledger.LedgerService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
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
  public ResponseEntity<Map<String, Object>> moderate(@Valid @RequestBody ModerationDecision decision)
      throws IOException {
    try {
      ledgerService.moderate(decision);
      return ResponseEntity.ok(Map.of("id", decision.getId(), "status", decision.getAction().name()));
    } catch (DuplicateReportException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
  }
}
