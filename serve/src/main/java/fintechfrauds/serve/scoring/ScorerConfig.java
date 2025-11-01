package fintechfrauds.serve.scoring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ScorerConfig {

  @Bean
  @Primary
  public Scorer scorer(org.springframework.core.env.Environment environment, DummyScorer dummyScorer)
      throws Exception {
    String type = environment.getProperty("fintechfrauds.model.type", "dummy").toLowerCase();
    if (!"xgb".equals(type)) {
      return dummyScorer;
    }

    String resourcePath = environment.getProperty("fintechfrauds.model.resourcePath", "models/model.xgb");
    ClassPathResource classPathResource = new ClassPathResource(resourcePath);
    if (!classPathResource.exists()) {
      throw new IllegalStateException(
          "XGBoost model not found at '%s'. Provide a trained artifact or switch fintechfrauds.model.type to 'dummy'."
              .formatted(resourcePath));
    }
    try (InputStream inputStream = classPathResource.getInputStream()) {
      byte[] modelBytes = inputStream.readAllBytes();
      Booster booster = XGBoost.loadModel(new ByteArrayInputStream(modelBytes));
      return new XgbScorer(booster);
    }
  }
}
