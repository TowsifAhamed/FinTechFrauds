package fintechfrauds.ledger;

public interface IdempotencyStore {
  void ensureNew(String key);
}
