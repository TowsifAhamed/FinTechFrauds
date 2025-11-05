package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.ScoreRequest;
import fintechfrauds.serve.api.dto.ScoreResponse;
import fintechfrauds.serve.logging.StructuredLogger;
import fintechfrauds.serve.scoring.FeatureStore;
import fintechfrauds.serve.scoring.FeatureVector;
import fintechfrauds.serve.scoring.RulesEngine;
import fintechfrauds.serve.scoring.Scorer;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/score")
@Validated
public class ScoreController {

  private static final Logger log = LoggerFactory.getLogger(ScoreController.class);
  private final FeatureStore featureStore;
  private final Scorer scorer;
  private final RulesEngine rulesEngine;

  public ScoreController(FeatureStore featureStore, Scorer scorer, RulesEngine rulesEngine) {
    this.featureStore = featureStore;
    this.scorer = scorer;
    this.rulesEngine = rulesEngine;
  }

  @PostMapping
  public ResponseEntity<ScoreResponse> score(@Valid @RequestBody ScoreRequest request) {
    String requestId = UUID.randomUUID().toString();
    long start = System.nanoTime();

    FeatureVector features = featureStore.loadFeatures(request);
    double risk = scorer.score(request, features);
    RulesEngine.DecisionResult decision = rulesEngine.evaluate(request, features, risk);
    ScoreResponse response =
        new ScoreResponse(decision.risk(), decision.decision(), decision.reasons());

    long elapsedMicros = (System.nanoTime() - start) / 1_000L;
    StructuredLogger.info(
        log,
        StructuredLogger.builder()
            .with("requestId", requestId)
            .with("api", "score")
            .with("status", 200)
            .with("latencyMicros", elapsedMicros)
            .with("decision", decision.decision())
            .with("risk", decision.risk())
            .with("reasons", decision.reasons())
            .with("accountHash", request.getAccountHash())
            .with("merchantHash", request.getMerchantHash())
            .build());

    return ResponseEntity.ok(response);
  }
}
