package fintechfrauds.serve.api;

import fintechfrauds.serve.api.dto.ScoreRequest;
import fintechfrauds.serve.api.dto.ScoreResponse;
import fintechfrauds.serve.scoring.RulesEngine;
import fintechfrauds.serve.scoring.ScoringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ScoreController {
  private final ScoringService scoringService;

  public ScoreController(ScoringService scoringService) {
    this.scoringService = scoringService;
  }

  @PostMapping("/score")
  public ScoreResponse score(@Valid @RequestBody ScoreRequest request) {
    RulesEngine.Decision decision =
        scoringService.score(
            request.accountHash(),
            request.epochMillis(),
            request.amountCents(),
            request.merchantHash(),
            request.mcc(),
            request.countryCode());
    return new ScoreResponse(decision.risk(), decision.action(), decision.reasons());
  }
}
