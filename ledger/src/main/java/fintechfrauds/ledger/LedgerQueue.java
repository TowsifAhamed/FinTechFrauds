package fintechfrauds.ledger;

public interface LedgerQueue {
  void enqueuePending(FraudReportPayload payload);
}
