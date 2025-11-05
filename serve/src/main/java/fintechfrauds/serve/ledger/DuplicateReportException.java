package fintechfrauds.serve.ledger;

public class DuplicateReportException extends RuntimeException {
  public DuplicateReportException(String message) {
    super(message);
  }
}
