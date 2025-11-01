package fintechfrauds.serve.scoring;

import fintechfrauds.serve.calibration.AccountCalibratorRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {
  private final FeatureStore featureStore;
  private final Scorer scorer;
  private final AccountCalibratorRepository calibratorRepository;

  public ScoringService(
      FeatureStore featureStore, Scorer scorer, AccountCalibratorRepository calibratorRepository) {
    this.featureStore = featureStore;
    this.scorer = scorer;
    this.calibratorRepository = calibratorRepository;
  }

  public RulesEngine.Decision score(
      String accountHash,
      long epochMillis,
      long amountCents,
      String merchantHash,
      String mcc,
      String countryCode) {
    Features features = featureStore.loadFeatures(accountHash);
    double amountLog = Math.log1p(Math.max(0, amountCents));
    FeatureVector vector =
        new FeatureVector(amountLog, features.amountZ(), features.window15mCount(), features.firstTimeMerchant() ? 1 : 0);

    double probability = scorer.score(vector);
    Instant at = Instant.ofEpochMilli(epochMillis);
    double calibrated = calibratorRepository
        .findActive(accountHash, at)
        .map(calibrator -> probability) // placeholder for future calibrator math
        .orElse(probability);
    return RulesEngine.apply(calibrated, features);
  }
}
