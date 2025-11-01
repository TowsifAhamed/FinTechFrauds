# FinTechFrauds

Reference implementation of a low-latency fraud detection stack built with Java first tooling.

## Project layout

```
core/      – canonical transaction types, hashing & validation helpers
features/  – Redis-backed rolling feature store
models/    – global model wrappers (XGBoost / ONNX) and per-account calibrators
rules/     – deterministic policy layer that augments model risk scores
ledger/    – moderated global fraud ledger domain objects & signature verification
serve/     – Spring Boot service exposing gRPC/HTTP entry points (WebFlux example)
storage/   – infrastructure helpers for Redis, Postgres, RocksDB
```

The root `pom.xml` wires all modules together so Maven can build them as a single multi-module project targeting Java 21.

## Data model & masking

`core` contains the `Txn` record with hashed identifiers, normalized description, and optional merchant metadata. `Hashing` centralizes SHA-256 hashing with tenant-specific salts, while `TxnValidator` rejects malformed or future-dated transactions before they reach downstream systems.

## Feature computation

`features` exposes a `FeatureStore` interface plus a `RedisFeatureStore` implementation that stores rolling aggregates in Redis hashes (`fs:acct:{accountHash}`). Feature snapshots deliver both numeric aggregates and timestamp metadata.

## Modeling

`models` packages a `Scorer` abstraction. The provided `XgbScorer` implements a compact logistic ensemble reader that matches the interfaces we would swap with XGBoost4J or ONNX Runtime in production, and `AccountCalibrator` offers a persistable per-account logistic layer for personalization.

## Rules

`rules` provides `RulesEngine` to layer velocity and amount heuristics on top of probabilistic outputs and return reason-coded `Decision` objects.

## Moderated fraud ledger

`ledger` describes the moderated ledger schema (`FraudReportPayload`, `LedgerEntry`) and utilities such as `SignatureVerifier`, `LedgerQueue`, and `IdempotencyStore` interfaces used by serving components.

## Serving surface

`serve` bootstraps a Spring Boot application that now exposes scoring and ledger moderation endpoints:

- `POST /v1/score` – loads Redis-backed features, applies the bundled `DummyScorer`, runs policy decisions via `RulesEngine`, and returns `{risk, decision, reasons}`.
- `POST /v1/ledger/report` – accepts signed fraud reports, enforces rate limits and idempotency, and enqueues the payload for human moderation while returning the server-assigned report `id`.
- `GET /v1/ledger/pending/next` – returns the next queued report (with payload) so analysts can inspect details before acting.
- `POST /v1/ledger/moderate` – lets a moderator approve or reject the oldest pending report, appending approved entries to `data/approved-ledger.jsonl`.
- `GET /v1/ledger/pending/count` – lightweight queue depth indicator for operations.

Accepted reports echo the generated `id`, acknowledge the enqueue with `queuedAt`, and every approved ledger line captures both values for downstream reconciliation.

Runtime security sits behind an `ApiAuthFilter` that verifies HMAC signatures (`X-Signature`) over `timestamp + "\n" + nonce + "\n" + sha256(body)` with shared secrets defined in `application.yml`. An in-memory token bucket applies per API key/IP limits, Redis ensures idempotency, and Actuator provides `/actuator/health` for readiness probes. `/v1/score` remains open for local experimentation; gate it with API keys before production use.

### Scoring configuration

- Toggle between the heuristic scorer and the bundled XGBoost model via `fintechfrauds.model.type` (`dummy` or `xgb`). The default keeps `/v1/score` running without native dependencies; switch to `xgb` once you install the accompanying JNI libraries.
- The repository does **not** bundle a pre-trained XGBoost artifact. When you want to exercise the `xgb` scorer, place a model at the location configured by `fintechfrauds.model.resourcePath` (defaults to `models/model.xgb`) before starting the service.
- XGBoost's native runtime depends on `libgomp1`. Install it on hosts (`sudo apt-get install libgomp1`) before toggling `fintechfrauds.model.type=xgb`; the provided Docker image installs it automatically.
- Seed Redis with realistic features using the CLI utility:

  ```bash
  mvn -q -pl serve exec:java \
    -Dexec.mainClass="fintechfrauds.serve.tools.BackfillFs" \
    -Dexec.args="--n=100 --prefix=acct_demo_"
  ```

  Pass `--csv=/path/to/features.csv` to ingest a CSV with headers `accountHash,amountZ,window15mCount,firstTimeMerchant,mcc`.

### Example calls

Score a transaction locally (no auth required by default):

```bash
curl -sS http://localhost:8080/v1/score \
  -H 'Content-Type: application/json' \
  -d '{
        "accountHash":"acct_demo",
        "epochMillis": 1761600000000,
        "description":"bought gift cards",
        "amountCents": 1000000,
        "merchantHash":"m_demo",
        "mcc":"6540",
        "countryCode":"US"
      }' | jq
```

Submit a fraud report (requires signing headers):

```bash
BODY='{
  "reportedAt":"2025-10-28T10:21:30Z",
  "reporter":"org_hash_demo",
  "merchantHash":"m_demo",
  "descriptionTokensHash":["t_gift","t_card","t_burst"],
  "mcc":"5999",
  "pattern":["BURST_GIFTCARDS"],
  "evidenceUri":"demo://evidence/123"
}'
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
NONCE=$(uuidgen | tr 'A-Z' 'a-z')
BH=$(printf "%s" "$BODY" | sha256sum | awk '{print $1}')
CANON="$TS\n$NONCE\n$BH"
SIG=$(printf "%s" "$CANON" | openssl dgst -sha256 -mac hmac -macopt key:demo_shared_secret_please_rotate -binary | base64)

curl -sS http://localhost:8080/v1/ledger/report \
  -H 'Content-Type: application/json' \
  -H 'X-Api-Key: demo_key' \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Idempotency-Key: $NONCE" \
  -H "X-Signature: $SIG" \
  -d "$BODY" | jq
# => {"status":"PENDING","id":"...","queued":true,"queuedAt":"2025-10-28T10:25:00Z"}
```

Moderate the next report in the queue:

```bash
curl -sS http://localhost:8080/v1/ledger/moderate \
  -H 'Content-Type: application/json' \
  -d '{"id":"<id-returned-from-report>","action":"APPROVE","moderator":"analyst_demo"}' | jq
```

Peek at the head of the moderation queue before deciding:

```bash
curl -sS http://localhost:8080/v1/ledger/pending/next \
  -H 'Content-Type: application/json' \
  -H 'X-Api-Key: demo_key' \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIG" | jq
```

### API exploration

- Swagger UI is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) and the OpenAPI specification at `/v3/api-docs`.
- The documentation routes bypass HMAC enforcement for ease of testing; disable them in production by setting `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`.

### Container images

Build the service image (multi-stage) from the repository root:

```bash
docker build -f serve/Dockerfile -t fintechfrauds/serve:dev .
```

Run it against a locally running Redis instance:

```bash
docker run --rm -p 8080:8080 \
  -e REDIS_HOST=host.docker.internal \
  -e FINTECHFRAUDS_SECURITY_APIKEYS_DEMO_KEY=demo_shared_secret_please_rotate \
  fintechfrauds/serve:dev
```

Or launch both Redis and the app with Docker Compose:

```bash
docker compose up --build
```

## Storage helpers

`storage` captures configuration records for Redis, Postgres, and RocksDB endpoints so runtime services can load connection details without embedding credentials in code.

## Building locally

Install Java 21+ and Maven, then run:

```
mvn -pl serve spring-boot:run
```

This launches the ledger API with sample in-memory dependencies. Update the configuration beans to wire Redis, Kafka, or databases as needed. The remaining modules compile independently and provide the baseline for extending features, models, and rule sets.

## Security & PII handling

- Account identifiers, merchant identifiers, and description tokens should be hashed client-side before calling the APIs. The reference `core` module documents the SHA-256 strategy and salt rotation policy.
- The scoring API only accepts hashed identifiers and plaintext transaction metadata required for rules.
- Fraud reports are signed with HMAC-SHA256 using tenant-specific secrets and enforced timestamp skew (`±5 minutes`) to mitigate replay.
- Enable TLS/Mutual TLS at the ingress layer and rotate API keys using your KMS of choice before promoting to production.
- Runtime API keys are normalized so that hyphenated and underscored variants both work. For example, setting `FINTECHFRAUDS_SECURITY_APIKEYS_DEMO_KEY` makes the service accept either `demo-key` or `demo_key` in the `X-Api-Key` header.

## Licensing

The project is dual-licensed. Community users operate under [PolyForm Noncommercial 1.0.0](licenses/PolyForm-Noncommercial-1.0.0.txt). Commercial adopters can request terms via the [commercial license](COMMERCIAL-LICENSE.md).
