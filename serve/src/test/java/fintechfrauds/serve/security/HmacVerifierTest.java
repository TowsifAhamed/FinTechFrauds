package fintechfrauds.serve.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class HmacVerifierTest {

  @Test
  void verifyAcceptsValidSignature() {
    String body = "{\"x\":1}";
    String timestamp = Instant.now().toString();
    String nonce = "abc123";
    String secret = "demo_shared_secret_please_rotate";

    String canonical =
        timestamp + "\n" + nonce + "\n" + sha256Hex(body.getBytes(StandardCharsets.UTF_8));
    String signature = hmacBase64(secret, canonical);

    boolean result =
        HmacVerifier.verify(
            signature, secret, timestamp, nonce, body.getBytes(StandardCharsets.UTF_8), 300);

    assertThat(result).isTrue();
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
      throw new IllegalStateException(e);
    }
  }

  private static String hmacBase64(String secret, String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] result = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
      return java.util.Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
