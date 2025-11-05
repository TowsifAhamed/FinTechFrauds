package fintechfrauds.serve.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class HmacVerifier {

  public String canonicalRequest(String timestamp, String nonce, String bodySha256Hex) {
    return timestamp + "\n" + nonce + "\n" + bodySha256Hex;
  }

  public String sha256Hex(String body) {
    return DigestUtils.sha256Hex(body == null ? "" : body);
  }

  public String sign(String secret, String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(raw);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Unable to compute HMAC", e);
    }
  }

  public boolean verifyTimestamp(String timestamp, long allowedSkewSeconds) {
    try {
      Instant instant = Instant.parse(timestamp);
      Instant now = Instant.now();
      return Math.abs(Duration.between(now, instant).getSeconds()) <= allowedSkewSeconds;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }
}
