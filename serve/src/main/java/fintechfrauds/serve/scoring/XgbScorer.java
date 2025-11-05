package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XgbScorer implements Scorer {

  private static final Logger log = LoggerFactory.getLogger(XgbScorer.class);

  private final Booster booster;

  public XgbScorer(Booster booster) {
    this.booster = booster;
  }

  public static XgbScorer fromPath(Path modelPath) throws IOException, XGBoostError {
    if (!Files.exists(modelPath)) {
      throw new IOException("Model path does not exist: " + modelPath);
    }
    Booster booster = XGBoost.loadModel(Files.newInputStream(modelPath));
    return new XgbScorer(booster);
  }

  @Override
  public double score(ScoreRequest request, FeatureVector features) {
    try {
      float[] values = buildFeatures(request, features);
      DMatrix matrix = new DMatrix(values, 1, values.length, Float.NaN);
      float[][] predictions = booster.predict(matrix);
      if (predictions.length == 0 || predictions[0].length == 0) {
        return 0.5d;
      }
      double raw = predictions[0][0];
      double risk = Math.max(0.01d, Math.min(0.99d, raw));
      return Math.round(risk * 1000.0d) / 1000.0d;
    } catch (XGBoostError e) {
      log.warn("xgb_scorer_predict_failed", e);
      return 0.5d;
    }
  }

  private float[] buildFeatures(ScoreRequest request, FeatureVector features) {
    float amount = request.getAmountCents() == null ? 0f : request.getAmountCents() / 1000.0f;
    float amountZ = (float) features.getAmountZ();
    float burst = features.getWindow15mCount();
    float first = features.getFirstTimeMerchant();
    float descLen = request.getDescription() == null ? 0f : request.getDescription().length();
    float country = request.getCountryCode() == null ? 0f : request.getCountryCode().hashCode() & 0x7fffffff;
    return new float[] {amount, amountZ, burst, first, descLen, country};
  }
}
