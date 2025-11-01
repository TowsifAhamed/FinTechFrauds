package fintechfrauds.serve.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
    @NotBlank String accountHash,
    @NotNull Long epochMillis,
    @NotBlank String description,
    @NotNull Long amountCents,
    String merchantHash,
    String mcc,
    String countryCode) {}
