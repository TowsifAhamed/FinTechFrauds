package fintechfrauds.serve.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fintechfrauds.serve.api.dto.FraudReportPayload;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {
  private final BlockingQueue<PendingReport> pendingQueue = new LinkedBlockingQueue<>();
  private final Path approvedPath;
  private final ObjectMapper objectMapper;

  public LedgerService(Environment environment) throws IOException {
    String fileLocation = environment.getProperty("fintechfrauds.ledger.approvedFile", "data/approved-ledger.jsonl");
    this.approvedPath = Paths.get(fileLocation);
    Path parent = approvedPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    if (!Files.exists(approvedPath)) {
      Files.createFile(approvedPath);
    }
    this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
  }

  public void enqueue(String id, Instant queuedAt, FraudReportPayload payload) {
    pendingQueue.offer(new PendingReport(id, queuedAt, payload));
  }

  public int pendingSize() {
    return pendingQueue.size();
  }

  public PendingReport peekPending() {
    return pendingQueue.peek();
  }

  public PendingReport pollPending() {
    return pendingQueue.poll();
  }

  public PendingReport takePending() throws InterruptedException {
    return pendingQueue.take();
  }

  public synchronized void approve(PendingReport report, String moderator) {
    ObjectNode node = objectMapper.valueToTree(report.payload());
    node.put("id", report.id());
    node.put("queuedAt", report.queuedAt().toString());
    node.put("status", "APPROVED");
    node.put("moderatedAt", Instant.now().toString());
    node.put("moderator", moderator);
    node.put("version", 1);
    append(node);
  }

  public void reject(PendingReport report) {
    // In a production system this would write to a rejected ledger or emit an audit event.
  }

  private void append(ObjectNode payload) {
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            approvedPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
      writer.write(objectMapper.writeValueAsString(payload));
      writer.newLine();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to append ledger entry", e);
    }
  }

  public record PendingReport(String id, Instant queuedAt, FraudReportPayload payload) {}
}
