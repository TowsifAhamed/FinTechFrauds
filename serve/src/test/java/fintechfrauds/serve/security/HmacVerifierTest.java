package fintechfrauds.serve.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmacVerifierTest {

  @Autowired private HmacVerifier hmacVerifier;

  @Test
  void computesSignatureConsistently() {
    String body = "{\"foo\":\"bar\"}";
    String timestamp = Instant.now().toString();
    String nonce = "abc123";
    String bodyHash = hmacVerifier.sha256Hex(body);
    String canonical = hmacVerifier.canonicalRequest(timestamp, nonce, bodyHash);
    String signature = hmacVerifier.sign("shared-secret", canonical);

    assertThat(signature).isNotBlank();
    assertThat(hmacVerifier.verifyTimestamp(timestamp, 300)).isTrue();
    assertThat(hmacVerifier.verifyTimestamp(Instant.now().minusSeconds(5000).toString(), 300))
        .isFalse();

    String tamperedCanonical =
        hmacVerifier.canonicalRequest(timestamp, nonce, hmacVerifier.sha256Hex("tampered"));
    String tamperedSignature = hmacVerifier.sign("shared-secret", tamperedCanonical);
    assertThat(tamperedSignature).isNotEqualTo(signature);

    String tamperedNonceCanonical =
        hmacVerifier.canonicalRequest(timestamp, nonce + "-tampered", bodyHash);
    String tamperedNonceSignature = hmacVerifier.sign("shared-secret", tamperedNonceCanonical);
    assertThat(tamperedNonceSignature).isNotEqualTo(signature);
  }
}
