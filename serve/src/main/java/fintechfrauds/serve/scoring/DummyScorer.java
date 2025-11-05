package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;
import org.springframework.stereotype.Component;

@Component
public class DummyScorer implements Scorer {

  @Override
  public double score(ScoreRequest request, FeatureVector features) {
    double base = 0.05d;
    double amountRisk = Math.min(0.5d, request.getAmountCents() / 300000.0d);
    double zRisk = Math.min(0.25d, Math.abs(features.getAmountZ()) / 10.0d);
    double burstRisk = Math.min(0.2d, features.getWindow15mCount() / 20.0d);
    double firstMerchantBoost = features.getFirstTimeMerchant() > 0 ? 0.1d : 0.0d;
    double merchantCategoryRisk = 0.0d;
    String descriptor = request.getDescription() == null ? "" : request.getDescription();
    if (descriptor.contains("GIFT") || descriptor.contains("STORED_VALUE")) {
      merchantCategoryRisk += 0.25d;
    }
    if (descriptor.contains("TRAVEL")
        || descriptor.contains("HOTEL")
        || descriptor.contains("AIRLINE")) {
      merchantCategoryRisk -= 0.1d;
    }
    double risk = base + amountRisk + zRisk + burstRisk + firstMerchantBoost + merchantCategoryRisk;
    if ("UNKNOWN".equalsIgnoreCase(features.getMcc()) && request.getMcc() != null) {
      risk += 0.05d;
    }
    risk = Math.max(0.01d, Math.min(0.99d, risk));
    return Math.round(risk * 1000.0d) / 1000.0d;
  }
}
