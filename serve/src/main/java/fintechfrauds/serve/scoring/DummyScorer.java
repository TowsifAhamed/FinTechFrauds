package fintechfrauds.serve.scoring;

import org.springframework.stereotype.Component;

@Component
public class DummyScorer implements Scorer {
  @Override
  public double score(FeatureVector vector) {
    double linear =
        -3.0
            + 0.3 * vector.amountZ()
            + 0.05 * vector.window15mCount()
            + 0.5 * vector.firstTimeMerchant();
    double base = sigmoid(linear);
    double adjusted = base + 0.02 * Math.tanh(vector.amountCentsLog() / 14.0);
    return Math.min(0.999, Math.max(0.001, adjusted));
  }

  private double sigmoid(double x) {
    return 1.0d / (1.0d + Math.exp(-x));
  }
}
