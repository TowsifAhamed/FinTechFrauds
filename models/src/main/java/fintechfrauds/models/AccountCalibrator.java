package fintechfrauds.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight per-account logistic calibrator that can be persisted.
 */
public final class AccountCalibrator {
  private final double[] weights;
  private final Instant startAt;
  private final Instant endAt;

  public AccountCalibrator(double[] weights, Instant startAt, Instant endAt) {
    this.weights = Objects.requireNonNull(weights);
    this.startAt = startAt;
    this.endAt = endAt;
  }

  public double calibrate(double globalScore, double[] localFeatures) {
    double[] combined = new double[localFeatures.length + 2];
    combined[0] = 1d; // bias term
    combined[1] = globalScore;
    System.arraycopy(localFeatures, 0, combined, 2, localFeatures.length);
    int len = Math.min(combined.length, weights.length);
    double sum = 0d;
    for (int i = 0; i < len; i++) {
      sum += combined[i] * weights[i];
    }
    return 1d / (1d + Math.exp(-sum));
  }

  public Instant startAt() {
    return startAt;
  }

  public Instant endAt() {
    return endAt;
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES * weights.length).order(ByteOrder.BIG_ENDIAN);
    for (double weight : weights) {
      buffer.putDouble(weight);
    }
    return buffer.array();
  }

  public static AccountCalibrator deserialize(byte[] bytes, Instant startAt, Instant endAt) {
    if (bytes.length % Double.BYTES != 0) {
      throw new IllegalArgumentException("invalid weight payload");
    }
    double[] decoded = new double[bytes.length / Double.BYTES];
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    for (int i = 0; i < decoded.length; i++) {
      decoded[i] = buffer.getDouble();
    }
    return new AccountCalibrator(decoded, startAt, endAt);
  }

  public static AccountCalibrator newDefault(int featureSize, Instant startAt, Instant endAt) {
    double[] weights = new double[featureSize + 2];
    return new AccountCalibrator(weights, startAt, endAt);
  }
}
