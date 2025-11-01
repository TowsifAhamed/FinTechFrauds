package fintechfrauds.serve.scoring;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

public class XgbScorer implements Scorer {
  private final Booster booster;

  public XgbScorer(Booster booster) {
    this.booster = booster;
  }

  @Override
  public double score(FeatureVector featureVector) {
    float[] row =
        new float[] {
          (float) featureVector.amountCentsLog(),
          (float) featureVector.amountZ(),
          (float) featureVector.window15mCount(),
          (float) featureVector.firstTimeMerchant()
        };
    DMatrix matrix = null;
    try {
      matrix = new DMatrix(row, 1, 4, Float.NaN);
      float[][] prediction = booster.predict(matrix);
      return prediction[0][0];
    } catch (XGBoostError error) {
      throw new RuntimeException("XGBoost scoring failed", error);
    } finally {
      if (matrix != null) {
        matrix.dispose();
      }
    }
  }
}
