package fintechfrauds.features;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable view of the currently aggregated features for an account.
 */
public record FeatureSnapshot(
    String accountHash,
    Map<String, Double> aggregates,
    Map<String, Instant> updatedAt
) {}
