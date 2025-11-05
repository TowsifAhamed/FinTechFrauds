package fintechfrauds.serve.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FraudReportPayload {

  @NotBlank
  private String reporter;

  @NotBlank
  private String accountHash;

  @NotBlank
  private String descriptionTokensHash;

  @NotBlank
  private String description;

  @NotNull
  @Min(0)
  private Long amountCents;

  private String merchantHash;
  private String countryCode;
  private Long reportedAt;

  public String getReporter() {
    return reporter;
  }

  public void setReporter(String reporter) {
    this.reporter = reporter;
  }

  public String getAccountHash() {
    return accountHash;
  }

  public void setAccountHash(String accountHash) {
    this.accountHash = accountHash;
  }

  public String getDescriptionTokensHash() {
    return descriptionTokensHash;
  }

  public void setDescriptionTokensHash(String descriptionTokensHash) {
    this.descriptionTokensHash = descriptionTokensHash;
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

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public Long getReportedAt() {
    return reportedAt;
  }

  public void setReportedAt(Long reportedAt) {
    this.reportedAt = reportedAt;
  }
}
