package fintechfrauds.serve.scoring;

public interface FeatureStore {
  Features loadFeatures(String accountHash);
}
