package fintechfrauds.serve.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class XgbScorerTest {

  @Test
  void loadsModelAndScores() throws Exception {
    ClassPathResource resource = new ClassPathResource("models/model.xgb");
    Assumptions.assumeTrue(resource.exists(), "XGBoost model fixture not present; skipping integration test");
    byte[] bytes;
    try (InputStream inputStream = resource.getInputStream()) {
      bytes = inputStream.readAllBytes();
    }
    Booster booster = XGBoost.loadModel(new ByteArrayInputStream(bytes));
    DMatrix matrix = null;
    try {
      matrix = new DMatrix(new float[] {12.3f, 0.5f, 3.0f, 1.0f}, 1, 4, Float.NaN);
      float[][] predictions = booster.predict(matrix);
      assertThat(predictions[0][0]).isBetween(0.0f, 1.0f);
    } finally {
      if (matrix != null) {
        matrix.dispose();
      }
    }
  }
}
