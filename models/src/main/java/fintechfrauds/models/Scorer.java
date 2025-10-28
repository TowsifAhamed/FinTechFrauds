package fintechfrauds.models;

public interface Scorer {
  double score(FeatureVector fv);
}
