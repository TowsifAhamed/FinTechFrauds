package fintechfrauds.serve.scoring;

public record FeatureVector(double amountCentsLog, double amountZ, int window15mCount, int firstTimeMerchant) {}
