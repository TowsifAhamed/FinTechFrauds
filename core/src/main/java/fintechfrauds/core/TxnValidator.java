package fintechfrauds.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Lightweight validation for incoming transactions.
 */
public final class TxnValidator {
  private TxnValidator() {}

  public static void ensureValid(Txn txn) {
    if (txn == null) {
      throw new IllegalArgumentException("txn must be non-null");
    }
    if (txn.accountHash() == null || txn.accountHash().isBlank()) {
      throw new IllegalArgumentException("accountHash is required");
    }
    if (txn.descriptionNorm() == null) {
      throw new IllegalArgumentException("descriptionNorm must be provided");
    }
    if (txn.amountCents() == 0L) {
      throw new IllegalArgumentException("amountCents must be non-zero");
    }
    Instant txnInstant = Instant.ofEpochMilli(txn.epochMillis());
    Instant cutoff = Instant.now().plus(1, ChronoUnit.DAYS);
    if (txnInstant.isAfter(cutoff)) {
      throw new IllegalArgumentException("future-dated transaction rejected");
    }
  }
}
