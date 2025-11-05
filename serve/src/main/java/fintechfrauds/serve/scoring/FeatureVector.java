package fintechfrauds.serve.scoring;

public class FeatureVector {
  private final double amountZ;
  private final int window15mCount;
  private final int firstTimeMerchant;
  private final String mcc;

  public FeatureVector(double amountZ, int window15mCount, int firstTimeMerchant, String mcc) {
    this.amountZ = amountZ;
    this.window15mCount = window15mCount;
    this.firstTimeMerchant = firstTimeMerchant;
    this.mcc = mcc;
  }

  public double getAmountZ() {
    return amountZ;
  }

  public int getWindow15mCount() {
    return window15mCount;
  }

  public int getFirstTimeMerchant() {
    return firstTimeMerchant;
  }

  public String getMcc() {
    return mcc;
  }
}
