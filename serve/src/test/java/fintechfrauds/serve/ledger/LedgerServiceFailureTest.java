package fintechfrauds.serve.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.api.dto.ModerationDecision;
import fintechfrauds.serve.config.FintechFraudsProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LedgerServiceFailureTest {

  @Test
  void appendFailureKeepsReportPending(@TempDir Path tempDir) throws Exception {
    Path blockingPath = tempDir.resolve("blocked-ledger.jsonl");
    FintechFraudsProperties properties = new FintechFraudsProperties();
    properties.getLedger().setApprovedFile(blockingPath.toString());

    LedgerService ledgerService = new LedgerService(new ObjectMapper(), properties);
    Files.createDirectory(blockingPath);

    FraudReportPayload payload = new FraudReportPayload();
    payload.setReporter("ops_team");
    payload.setAccountHash("acct_hash");
    payload.setDescriptionTokensHash("desc_tokens");
    payload.setDescription("STORED_VALUE_PROVIDER");
    payload.setAmountCents(5000L);

    LedgerService.PendingReport pending = ledgerService.enqueue(payload);

    ModerationDecision decision = new ModerationDecision();
    decision.setId(pending.id());
    decision.setAction(ModerationDecision.Action.APPROVE);
    decision.setModerator("moderator");

    assertThatThrownBy(() -> ledgerService.moderate(decision))
        .isInstanceOf(IOException.class);

    assertThat(ledgerService.pendingCount()).isEqualTo(1);
    assertThat(ledgerService.peekNext()).contains(pending);
  }
}
