package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;

public interface FeatureStore {
  FeatureVector loadFeatures(ScoreRequest request);
}
