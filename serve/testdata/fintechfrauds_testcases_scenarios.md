# FinTechFrauds Test Scenarios (masked)

All records are **pseudonymized**. `accountHash` values are synthetic, `description` is normalized (no raw merchant text), and `merchantHash` is a one‑way hash of the normalized descriptor.

## Groups
- **A — Routine spend**: supermarkets, small food, micro‑vending — label `legit`.
- **B — Kiosk micro‑bursts**: repeated $2–$5 charges within minutes — `fraud` in burst case.
- **C — Stored‑value/gift card burst**: 24 loads on a single day in two clusters — `fraud`.
- **D — Single huge item**: one‑off $10k electronics purchase — `fraud`.
- **E — First‑time merchant, high ticket** — `fraud`.
- **F — Travel spike**: airline + hotel in same week — `legit` but anomaly‑worthy.
- **G — Deposits**: payroll or rideshare payouts — `legit` income signals.
- **H — Duplicate auth**: near‑duplicate time/amount — should dedupe downstream.
- **I — Geo anomaly**: remote small auth following local fuel — `fraud`.

## Field schema
- `accountHash` (string) — masked customer handle
- `epochMillis` (int) — UTC epoch ms
- `description` (string) — normalized descriptor (category-like)
- `amountCents` (int)
- `merchantHash` (string) — 16-hex masked hash
- `mcc` (string) — indicative MCC
- `countryCode` (string)
- `scenarioId` (string) — scenario tag
- `label` (string) — `fraud` or `legit`
- `notes` (string) — short rationale
