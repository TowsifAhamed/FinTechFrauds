package fintechfrauds.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Lightweight scorer that mimics the signature of an XGBoost-backed scorer.
 *
 * <p>The constructor accepts the serialized weights of a single decision tree
 * ensemble encoded as a sequence of doubles (bias + feature weights). In a
 * production deployment replace this class with the actual XGBoost4J or ONNX
 * runtime wrapper.</p>
 */
public final class XgbScorer implements Scorer {
  private final double[] weights;

  public XgbScorer(byte[] modelBytes) {
    this.weights = decode(modelBytes);
  }

  @Override
  public double score(FeatureVector fv) {
    double[] values = fv.values();
    double sum = weights[0];
    int len = Math.min(values.length, weights.length - 1);
    for (int i = 0; i < len; i++) {
      sum += values[i] * weights[i + 1];
    }
    return 1d / (1d + Math.exp(-sum));
  }

  private static double[] decode(byte[] bytes) {
    if (bytes.length % Double.BYTES != 0) {
      throw new IllegalArgumentException("model bytes must align to doubles");
    }
    double[] decoded = new double[bytes.length / Double.BYTES];
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    for (int i = 0; i < decoded.length; i++) {
      decoded[i] = buffer.getDouble();
    }
    if (decoded.length == 0) {
      decoded = new double[] {0d};
    }
    return decoded;
  }

  @Override
  public String toString() {
    return "XgbScorer" + Arrays.toString(weights);
  }
}
