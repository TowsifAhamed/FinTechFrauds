package fintechfrauds.core;

/**
 * Canonical representation of an incoming transaction after masking and normalization.
 */
public record Txn(
    String accountHash,
    long epochMillis,
    String descriptionNorm,
    long amountCents,
    String merchantHash,
    String mcc,
    String countryCode
) {}
