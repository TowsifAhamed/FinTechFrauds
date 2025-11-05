package fintechfrauds.serve.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;

/** Utility for emitting structured JSON logs without leaking PII. */
public final class StructuredLogger {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  private StructuredLogger() {}

  public static Builder builder() {
    return new Builder();
  }

  public static void info(Logger logger, Map<String, ?> event) {
    if (!logger.isInfoEnabled()) {
      return;
    }
    try {
      logger.info(MAPPER.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      logger.info(event.toString());
    }
  }

  public static final class Builder {
    private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

    private Builder() {}

    public Builder with(String key, Object value) {
      if (value != null) {
        fields.put(key, value);
      }
      return this;
    }

    public Builder withNullable(String key, Object value) {
      fields.put(key, value);
      return this;
    }

    public Map<String, Object> build() {
      return fields;
    }
  }
}
