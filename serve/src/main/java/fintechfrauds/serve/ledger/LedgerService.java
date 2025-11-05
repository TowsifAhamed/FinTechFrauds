package fintechfrauds.serve.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fintechfrauds.serve.api.dto.FraudReportPayload;
import fintechfrauds.serve.api.dto.ModerationDecision;
import fintechfrauds.serve.config.FintechFraudsProperties;
import fintechfrauds.serve.logging.StructuredLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

  private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
  private static final DateTimeFormatter DAY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private final ObjectMapper objectMapper;
  private final Path approvedPath;
  private final JsonSchema approvedSchema;
  private final ConcurrentLinkedQueue<PendingReport> queue = new ConcurrentLinkedQueue<>();
  private final Map<String, PendingReport> pendingById = new ConcurrentHashMap<>();
  private final Map<String, String> dedupeIndex = new ConcurrentHashMap<>();
  private final java.util.Set<String> approvedDedupeKeys = ConcurrentHashMap.newKeySet();
  private final AtomicReference<String> lastHash = new AtomicReference<>(null);

  public LedgerService(ObjectMapper objectMapper, FintechFraudsProperties properties)
      throws IOException {
    this.objectMapper = objectMapper.copy();
    this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    this.approvedPath = resolveApprovedPath(properties.getLedger().getApprovedFile());
    this.approvedSchema = loadSchema();
    initializeLastHash();
  }

  public PendingReport enqueue(FraudReportPayload payload) {
    String dedupeKey = buildDedupeKey(payload);
    if (approvedDedupeKeys.contains(dedupeKey)) {
      StructuredLogger.info(
          log,
          StructuredLogger.builder()
              .with("event", "ledger_duplicate_approved")
              .with("reporter", payload.getReporter())
              .with("merchantHash", payload.getMerchantHash())
              .with("dedupeKey", dedupeKey)
              .build());
      throw new DuplicateReportException("Duplicate report already approved for this day");
    }
    if (dedupeIndex.containsKey(dedupeKey)) {
      String existingId = dedupeIndex.get(dedupeKey);
      PendingReport existing = existingId == null ? null : pendingById.get(existingId);
      StructuredLogger.info(
          log,
          StructuredLogger.builder()
              .with("event", "ledger_duplicate_report")
              .with("reporter", payload.getReporter())
              .with("merchantHash", payload.getMerchantHash())
              .with("dedupeKey", dedupeKey)
              .with("existingId", existingId)
              .build());
      if (existing != null) {
        return existing;
      }
    }
    String id = UUID.randomUUID().toString();
    PendingReport report = new PendingReport(id, payload, Instant.now(), dedupeKey);
    queue.offer(report);
    pendingById.put(id, report);
    dedupeIndex.put(dedupeKey, id);
    StructuredLogger.info(
        log,
        StructuredLogger.builder()
            .with("event", "ledger_report_enqueued")
            .with("reportId", id)
            .with("reporter", payload.getReporter())
            .with("merchantHash", payload.getMerchantHash())
            .with("dedupeKey", dedupeKey)
            .build());
    return report;
  }

  public int pendingCount() {
    return pendingById.size();
  }

  public Optional<PendingReport> peekNext() {
    return Optional.ofNullable(queue.peek());
  }

  public synchronized void moderate(ModerationDecision decision) throws IOException {
    PendingReport report = pendingById.get(decision.getId());
    if (report == null) {
      throw new IllegalArgumentException("Unknown report id: " + decision.getId());
    }
    if (decision.getAction() == ModerationDecision.Action.APPROVE) {
      if (approvedDedupeKeys.contains(report.dedupeKey())) {
        StructuredLogger.info(
            log,
            StructuredLogger.builder()
                .with("event", "ledger_duplicate_on_moderation")
                .with("reportId", report.id())
                .with("dedupeKey", report.dedupeKey())
                .with("moderator", decision.getModerator())
                .build());
        throw new DuplicateReportException("Duplicate report already approved for this day");
      }
      appendApproved(report, decision.getModerator());
    } else {
      StructuredLogger.info(
          log,
          StructuredLogger.builder()
              .with("event", "ledger_report_rejected")
              .with("reportId", decision.getId())
              .with("moderator", decision.getModerator())
              .build());
    }
    pendingById.remove(report.id());
    queue.remove(report);
    dedupeIndex.remove(report.dedupeKey());
  }

  public record PendingReport(String id, FraudReportPayload payload, Instant receivedAt, String dedupeKey) {}

  private void appendApproved(PendingReport report, String moderator) throws IOException {
    if (approvedPath.getParent() != null) {
      Files.createDirectories(approvedPath.getParent());
    }
    ObjectNode node = objectMapper.createObjectNode();
    node.put("id", report.id());
    node.put("reporter", report.payload().getReporter());
    node.put("accountHash", report.payload().getAccountHash());
    if (report.payload().getMerchantHash() != null) {
      node.put("merchantHash", report.payload().getMerchantHash());
    } else {
      node.putNull("merchantHash");
    }
    node.put("descriptionTokensHash", report.payload().getDescriptionTokensHash());
    node.put("description", report.payload().getDescription());
    node.put("amountCents", report.payload().getAmountCents());
    if (report.payload().getCountryCode() != null) {
      node.put("countryCode", report.payload().getCountryCode());
    } else {
      node.putNull("countryCode");
    }
    if (report.payload().getReportedAt() != null) {
      node.put("reportedAt", report.payload().getReportedAt());
    } else {
      node.putNull("reportedAt");
    }
    node.put("status", "APPROVED");
    node.put("moderatedAt", Instant.now().toString());
    node.put("moderator", moderator);
    node.put("version", 1);
    String prevHashValue = lastHash.get();
    if (prevHashValue == null) {
      node.putNull("prevHash");
    } else {
      node.put("prevHash", prevHashValue);
    }

    String canonical = objectMapper.writeValueAsString(node);
    String hash = sha256((prevHashValue == null ? "" : prevHashValue) + canonical);
    node.put("hash", hash);

    String finalLine = objectMapper.writeValueAsString(node);
    validateAgainstSchema(finalLine);
    Files.writeString(approvedPath, finalLine + System.lineSeparator(), StandardCharsets.UTF_8,
        Files.exists(approvedPath)
            ? java.nio.file.StandardOpenOption.APPEND
            : java.nio.file.StandardOpenOption.CREATE);
    lastHash.set(hash);
    approvedDedupeKeys.add(report.dedupeKey());
    StructuredLogger.info(
        log,
        StructuredLogger.builder()
            .with("event", "ledger_report_approved")
            .with("reportId", report.id())
            .with("hash", hash)
            .withNullable("prevHash", prevHashValue)
            .with("moderator", moderator)
            .build());
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private JsonSchema loadSchema() throws IOException {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    Path schemaPath = locateSchemaPath();
    if (schemaPath != null) {
      try (var stream = Files.newInputStream(schemaPath)) {
        return factory.getSchema(stream);
      }
    }
    var resource = new org.springframework.core.io.ClassPathResource("ledger/approved-ledger.schema.json");
    if (resource.exists()) {
      try (var stream = resource.getInputStream()) {
        return factory.getSchema(stream);
      }
    }
    throw new IOException("approved-ledger.schema.json not found on filesystem or classpath");
  }

  private Path locateSchemaPath() {
    Path[] candidates = {
      Paths.get("ledger/approved-ledger.schema.json"),
      Paths.get("..", "ledger", "approved-ledger.schema.json"),
      Paths.get("../..", "ledger", "approved-ledger.schema.json")
    };
    for (Path candidate : candidates) {
      Path normalized = candidate.toAbsolutePath().normalize();
      if (Files.exists(normalized)) {
        return normalized;
      }
    }
    return null;
  }

  private void validateAgainstSchema(String json) throws IOException {
    JsonNode node = objectMapper.readTree(json);
    var errors = approvedSchema.validate(node);
    if (!errors.isEmpty()) {
      throw new IOException("Ledger entry failed schema validation: " + errors);
    }
  }

  private void initializeLastHash() throws IOException {
    if (!Files.exists(approvedPath)) {
      return;
    }
    var lines = Files.readAllLines(approvedPath, StandardCharsets.UTF_8);
    if (lines.isEmpty()) {
      return;
    }
    for (String raw : lines) {
      String trimmed = raw.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      JsonNode node = objectMapper.readTree(trimmed);
      lastHash.set(node.path("hash").isMissingNode() || node.path("hash").isNull() ? null : node.path("hash").asText());
      approvedDedupeKeys.add(dedupeKeyFromNode(node));
    }
  }

  private Path resolveApprovedPath(String configured) {
    Path configuredPath = Paths.get(configured);
    if (configuredPath.isAbsolute()) {
      return configuredPath;
    }
    Path moduleRoot = Paths.get("serve").toAbsolutePath().normalize();
    return moduleRoot.resolve(configuredPath).normalize();
  }

  private String buildDedupeKey(FraudReportPayload payload) {
    Instant reference = payload.getReportedAt() != null
        ? Instant.ofEpochMilli(payload.getReportedAt())
        : Instant.now();
    return buildDedupeKey(
        payload.getReporter(), payload.getMerchantHash(), payload.getDescriptionTokensHash(), reference);
  }

  private String buildDedupeKey(
      String reporter, String merchantHash, String descriptionTokensHash, Instant referenceInstant) {
    String day = DAY_FORMATTER.format(referenceInstant);
    return String.join(
        "|",
        reporter,
        Optional.ofNullable(merchantHash).orElse("NONE"),
        Optional.ofNullable(descriptionTokensHash).orElse("NONE"),
        day);
  }

  private String dedupeKeyFromNode(JsonNode node) {
    String reporter = node.path("reporter").asText("");
    String merchant = node.path("merchantHash").isNull() ? null : node.path("merchantHash").asText(null);
    String descriptor = node.path("descriptionTokensHash").asText("");
    Instant reference;
    if (node.hasNonNull("reportedAt")) {
      reference = Instant.ofEpochMilli(node.path("reportedAt").asLong());
    } else {
      reference = Instant.parse(node.path("moderatedAt").asText());
    }
    return buildDedupeKey(reporter, merchant, descriptor, reference);
  }
}
