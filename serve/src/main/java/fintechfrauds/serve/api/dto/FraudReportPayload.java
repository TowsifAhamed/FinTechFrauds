package fintechfrauds.serve.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FraudReportPayload(
    @NotBlank String reportedAt,
    @NotBlank String reporter,
    String accountHash,
    String merchantHash,
    List<String> descriptionTokensHash,
    String mcc,
    List<String> pattern,
    String evidenceUri,
    String signature,
    String status,
    String moderatedAt,
    String moderator,
    Integer version) {}
