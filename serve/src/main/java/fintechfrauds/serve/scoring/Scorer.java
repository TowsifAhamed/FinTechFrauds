package fintechfrauds.serve.scoring;

import fintechfrauds.serve.api.dto.ScoreRequest;

public interface Scorer {
  double score(ScoreRequest request, FeatureVector features);
}
