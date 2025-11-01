package fintechfrauds.serve.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.time.format.DateTimeParseException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacVerifier {
  private HmacVerifier() {}

  public static boolean verify(
      String providedBase64,
      String secret,
      String timestampIso,
      String nonce,
      byte[] body,
      long allowedSkewSeconds) {
    if (secret == null) {
      return false;
    }
    String bodyHash = sha256Hex(body);
    String canonical = timestampIso + "\n" + nonce + "\n" + bodyHash;
    String expected = hmacBase64(secret, canonical);
    if (!Objects.equals(expected, providedBase64)) {
      return false;
    }
    try {
      Instant timestamp = Instant.parse(timestampIso);
      Instant now = Instant.now();
      long delta = Math.abs(Duration.between(timestamp, now).getSeconds());
      return delta <= allowedSkewSeconds;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  private static String hmacBase64(String secret, String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] result = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute signature", e);
    }
  }

  private static String sha256Hex(byte[] body) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(body);
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash request body", e);
    }
  }
}
