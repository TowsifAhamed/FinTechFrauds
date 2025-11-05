package fintechfrauds.serve.api.dto;

import java.util.List;

public class ScoreResponse {
  private double risk;
  private String decision;
  private List<String> reasons;

  public ScoreResponse() {}

  public ScoreResponse(double risk, String decision, List<String> reasons) {
    this.risk = risk;
    this.decision = decision;
    this.reasons = reasons;
  }

  public double getRisk() {
    return risk;
  }

  public void setRisk(double risk) {
    this.risk = risk;
  }

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public List<String> getReasons() {
    return reasons;
  }

  public void setReasons(List<String> reasons) {
    this.reasons = reasons;
  }
}
