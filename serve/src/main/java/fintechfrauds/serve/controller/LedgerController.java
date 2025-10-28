package fintechfrauds.serve.controller;

import fintechfrauds.ledger.FraudReportPayload;
import fintechfrauds.ledger.IdempotencyStore;
import fintechfrauds.ledger.LedgerQueue;
import fintechfrauds.ledger.SignatureVerifier;
import fintechfrauds.serve.RateLimiterService;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ledger")
public class LedgerController {
  private final RateLimiterService rateLimiter;
  private final SignatureVerifier signatureVerifier;
  private final LedgerQueue queue;
  private final IdempotencyStore idempotencyStore;

  public LedgerController(RateLimiterService rateLimiter, SignatureVerifier signatureVerifier,
                          LedgerQueue queue, IdempotencyStore idempotencyStore) {
    this.rateLimiter = rateLimiter;
    this.signatureVerifier = signatureVerifier;
    this.queue = queue;
    this.idempotencyStore = idempotencyStore;
  }

  @PostMapping("/report")
  public ResponseEntity<Map<String, String>> report(
      @RequestHeader("X-Api-Key") String apiKey,
      @RequestHeader("X-Idempotency-Key") String idempotencyKey,
      @RequestBody FraudReportPayload payload) {
    rateLimiter.check(apiKey);
    signatureVerifier.verify(payload);
    idempotencyStore.ensureNew(idempotencyKey);
    queue.enqueuePending(payload);
    return ResponseEntity.accepted().body(Map.of(
        "status", "PENDING",
        "receivedAt", Instant.now().toString()));
  }
}
