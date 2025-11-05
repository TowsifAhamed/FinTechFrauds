package fintechfrauds.serve.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ModerationDecision {

  @NotBlank
  private String id;

  @NotNull
  private Action action;

  @NotBlank
  private String moderator;

  public enum Action {
    APPROVE,
    REJECT
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public String getModerator() {
    return moderator;
  }

  public void setModerator(String moderator) {
    this.moderator = moderator;
  }
}
