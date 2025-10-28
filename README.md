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

`serve` bootstraps a Spring Boot WebFlux application with a `LedgerController` to accept fraud reports via `/v1/ledger/report`. Requests are gated by a lightweight `RateLimiterService`, HMAC verification, and idempotency tracking before being enqueued for moderation. `LedgerConfig` supplies in-memory defaults ready to be swapped for production queue/idempotency implementations and external rate limiters such as Bucket4j.

## Storage helpers

`storage` captures configuration records for Redis, Postgres, and RocksDB endpoints so runtime services can load connection details without embedding credentials in code.

## Building locally

Install Java 21+ and Maven, then run:

```
mvn -pl serve spring-boot:run
```

This launches the ledger API with sample in-memory dependencies. Update the configuration beans to wire Redis, Kafka, or databases as needed. The remaining modules compile independently and provide the baseline for extending features, models, and rule sets.
