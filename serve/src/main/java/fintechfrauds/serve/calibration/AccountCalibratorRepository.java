package fintechfrauds.serve.calibration;

import java.time.Instant;
import java.util.Optional;

public interface AccountCalibratorRepository {
  Optional<Calibrator> findActive(String accountHash, Instant at);

  void save(Calibrator calibrator);

  record Calibrator(String accountHash, int version, Instant startAt, Instant endAt, byte[] weightsBlob) {}
}
