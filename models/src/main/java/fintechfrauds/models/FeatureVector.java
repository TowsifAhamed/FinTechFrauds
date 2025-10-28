package fintechfrauds.models;

/**
 * Dense feature vector wrapper for scoring pipeline.
 */
public record FeatureVector(double[] values) {
  public float[][] toDenseMatrix() {
    float[] row = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      row[i] = (float) values[i];
    }
    return new float[][] { row };
  }
}
