package fintechfrauds.serve.calibration;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAccountCalibratorRepository implements AccountCalibratorRepository {
  private final Map<String, Calibrator> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Calibrator> findActive(String accountHash, Instant at) {
    Calibrator calibrator = store.get(accountHash);
    if (calibrator == null) {
      return Optional.empty();
    }
    boolean active =
        (at.equals(calibrator.startAt()) || at.isAfter(calibrator.startAt()))
            && (at.equals(calibrator.endAt()) || at.isBefore(calibrator.endAt()));
    return active ? Optional.of(calibrator) : Optional.empty();
  }

  @Override
  public void save(Calibrator calibrator) {
    store.put(calibrator.accountHash(), calibrator);
  }
}
