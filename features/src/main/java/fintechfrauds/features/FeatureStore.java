package fintechfrauds.features;

import fintechfrauds.core.Txn;
import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over the rolling feature store backed by Redis.
 */
public interface FeatureStore {
  void update(Txn txn);

  Optional<FeatureSnapshot> load(String accountHash);

  void expire(String accountHash, Duration ttl);
}
