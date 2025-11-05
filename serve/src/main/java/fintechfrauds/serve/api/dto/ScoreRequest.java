package fintechfrauds.serve.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreRequest {

  @NotBlank
  private String accountHash;

  @NotNull
  private Long epochMillis;

  @NotBlank
  @Size(max = 128)
  private String description;

  @NotNull
  @Min(0)
  private Long amountCents;

  private String merchantHash;

  private String mcc;

  private String countryCode;

  public String getAccountHash() {
    return accountHash;
  }

  public void setAccountHash(String accountHash) {
    this.accountHash = accountHash;
  }

  public Long getEpochMillis() {
    return epochMillis;
  }

  public void setEpochMillis(Long epochMillis) {
    this.epochMillis = epochMillis;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(Long amountCents) {
    this.amountCents = amountCents;
  }

  public String getMerchantHash() {
    return merchantHash;
  }

  public void setMerchantHash(String merchantHash) {
    this.merchantHash = merchantHash;
  }

  public String getMcc() {
    return mcc;
  }

  public void setMcc(String mcc) {
    this.mcc = mcc;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }
}
