package fintechfrauds.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Salted hashing helper that keeps raw identifiers out of downstream systems.
 */
public final class Hashing {
  private Hashing() {}

  public static String sha256Hex(String salt, String value) {
    if (salt == null || value == null) {
      throw new IllegalArgumentException("Salt and value must be non-null");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt.getBytes(StandardCharsets.UTF_8));
      digest.update(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
