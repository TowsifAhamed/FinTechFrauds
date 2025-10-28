package fintechfrauds.ledger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SignatureVerifier {
  private final byte[] secret;

  public SignatureVerifier(byte[] secret) {
    this.secret = secret.clone();
  }

  public void verify(FraudReportPayload payload) {
    String expected = compute(payload);
    if (!expected.equals(payload.signature())) {
      throw new IllegalArgumentException("Invalid signature");
    }
  }

  private String compute(FraudReportPayload payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      String body = payload.id() + "|" + payload.reportedAt();
      return Base64.getEncoder().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to verify signature", e);
    }
  }
}
