package fintechfrauds.serve.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fintechfrauds")
public class FintechFraudsProperties {

  private RateLimits rateLimits = new RateLimits();
  private Security security = new Security();
  private Model model = new Model();
  private Ledger ledger = new Ledger();

  public RateLimits getRateLimits() {
    return rateLimits;
  }

  public void setRateLimits(RateLimits rateLimits) {
    this.rateLimits = rateLimits;
  }

  public Security getSecurity() {
    return security;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Model getModel() {
    return model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public Ledger getLedger() {
    return ledger;
  }

  public void setLedger(Ledger ledger) {
    this.ledger = ledger;
  }

  public static class RateLimits {
    private long capacity = 60;
    private long refillTokens = 60;
    private long refillPeriodSeconds = 60;

    public long getCapacity() {
      return capacity;
    }

    public void setCapacity(long capacity) {
      this.capacity = capacity;
    }

    public long getRefillTokens() {
      return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
      this.refillTokens = refillTokens;
    }

    public long getRefillPeriodSeconds() {
      return refillPeriodSeconds;
    }

    public void setRefillPeriodSeconds(long refillPeriodSeconds) {
      this.refillPeriodSeconds = refillPeriodSeconds;
    }
  }

  public static class Security {
    private long requestTimeSkewSeconds = 300;
    private Map<String, String> apiKeys = new HashMap<>();

    public long getRequestTimeSkewSeconds() {
      return requestTimeSkewSeconds;
    }

    public void setRequestTimeSkewSeconds(long requestTimeSkewSeconds) {
      this.requestTimeSkewSeconds = requestTimeSkewSeconds;
    }

    public Map<String, String> getApiKeys() {
      return apiKeys;
    }

    public void setApiKeys(Map<String, String> apiKeys) {
      this.apiKeys = apiKeys;
    }
  }

  public static class Model {
    private String type = "dummy";
    private String resourcePath = "models/model.xgb";
    private URI uri;
    private String sha256;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getResourcePath() {
      return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
    }

    public URI getUri() {
      return uri;
    }

    public void setUri(URI uri) {
      this.uri = uri;
    }

    public String getSha256() {
      return sha256;
    }

    public void setSha256(String sha256) {
      this.sha256 = sha256;
    }
  }

  public static class Ledger {
    private String approvedFile = "data/approved-ledger.jsonl";

    public String getApprovedFile() {
      return approvedFile;
    }

    public void setApprovedFile(String approvedFile) {
      this.approvedFile = approvedFile;
    }
  }
}
