package fintechfrauds.rules;

import fintechfrauds.features.FeatureSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule set layered on top of ML scores.
 */
public final class RulesEngine {
  private RulesEngine() {}

  public static Decision apply(double modelProbability, FeatureSnapshot features) {
    List<String> reasons = new ArrayList<>();
    double amountZ = features.aggregates().getOrDefault("amount_z", 0d);
    double burst15m = features.aggregates().getOrDefault("count_15m", 0d);
    boolean firstMerchant = features.aggregates().getOrDefault("first_merchant", 0d) > 0.5;
    if (amountZ > 6 && firstMerchant) {
      reasons.add("AMOUNT_OUTLIER_FIRST_MERCHANT");
    }
    if (burst15m > 20) {
      reasons.add("BURST_ACTIVITY_15M");
    }
    String action;
    if (modelProbability > 0.95 || reasons.contains("BURST_ACTIVITY_15M")) {
      action = "DECLINE";
    } else if (modelProbability > 0.80 || !reasons.isEmpty()) {
      action = "REVIEW";
    } else {
      action = "APPROVE";
    }
    return new Decision(modelProbability, action, reasons);
  }
}
