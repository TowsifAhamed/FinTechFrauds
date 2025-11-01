package fintechfrauds.serve.scoring;

public interface Scorer {
  double score(FeatureVector vector);
}
