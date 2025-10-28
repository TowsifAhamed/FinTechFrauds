package fintechfrauds.ledger;

import java.time.Instant;
import java.util.List;

public record LedgerEntry(
    String id,
    Instant reportedAt,
    String reporter,
    String accountHash,
    String merchantHash,
    List<String> descriptionTokensHash,
    String mcc,
    List<String> pattern,
    String evidenceUri,
    String signature,
    String status,
    Instant moderatedAt,
    String moderator,
    int version
) {}
