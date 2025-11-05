package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RulesEngine {

  public DecisionResult evaluate(ScoreRequest request, FeatureVector features, double risk) {
    List<String> reasons = new ArrayList<>();
    String descriptor = request.getDescription() == null ? "" : request.getDescription();

    if (features.getFirstTimeMerchant() > 0 && request.getAmountCents() > 50000) {
      reasons.add("AMOUNT_OUTLIER_FIRST_MERCHANT");
    }
    boolean storedValue = descriptor.contains("GIFT") || descriptor.contains("STORED_VALUE");
    if (features.getWindow15mCount() >= 3 && storedValue) {
      reasons.add("BURST_GIFTCARDS");
    }
    if (risk > 0.9d) {
      reasons.add("MODEL_RISK_HIGH");
    }
    if (risk > 0.75d && features.getFirstTimeMerchant() > 0) {
      reasons.add("MODEL_RISK_FIRST_MERCHANT");
    }

    String decision;
    if (risk >= 0.85d || reasons.contains("BURST_GIFTCARDS")) {
      if (risk >= 0.85d && reasons.stream().noneMatch(reason -> reason.startsWith("MODEL_RISK"))) {
        reasons.add("MODEL_RISK_EXTREME");
      }
      decision = "DECLINE";
    } else if (risk >= 0.55d || !reasons.isEmpty()) {
      if (risk >= 0.55d && reasons.isEmpty()) {
        reasons.add("MODEL_RISK_ELEVATED");
      }
      decision = "REVIEW";
    } else {
      decision = "APPROVE";
    }

    return new DecisionResult(risk, decision, Collections.unmodifiableList(reasons));
  }

  public record DecisionResult(double risk, String decision, List<String> reasons) {}
}
