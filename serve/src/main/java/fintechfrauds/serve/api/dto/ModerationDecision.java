package fintechfrauds.serve.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerationDecision(
    @NotBlank String id,
    @NotBlank String action,
    @NotBlank String moderator) {}
