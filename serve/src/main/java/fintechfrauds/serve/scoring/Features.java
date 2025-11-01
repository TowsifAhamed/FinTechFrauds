package fintechfrauds.serve.scoring;

public record Features(double amountZ, int window15mCount, boolean firstTimeMerchant, String mcc) {
  public boolean mccEquals(String code) {
    return code != null && code.equals(mcc);
  }
}
