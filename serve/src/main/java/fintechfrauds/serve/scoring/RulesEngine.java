package fintechfrauds.serve.scoring;

import java.util.ArrayList;
import java.util.List;

public final class RulesEngine {
  private RulesEngine() {}

  public static Decision apply(double probability, Features features) {
    List<String> reasons = new ArrayList<>();
    if (features.amountZ() > 6.0 && features.firstTimeMerchant()) {
      reasons.add("AMOUNT_OUTLIER_FIRST_MERCHANT");
    }
    if (features.window15mCount() > 20 && features.mccEquals("6540")) {
      reasons.add("BURST_GIFTCARDS");
    }
    String action;
    if (probability > 0.95 || reasons.contains("BURST_GIFTCARDS")) {
      action = "DECLINE";
    } else if (probability > 0.80) {
      action = "REVIEW";
    } else {
      action = "APPROVE";
    }
    return new Decision(probability, action, reasons);
  }

  public record Decision(double risk, String action, List<String> reasons) {}
}
