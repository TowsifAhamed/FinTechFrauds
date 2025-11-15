# Central Fraud Ledger Analysis

**Date:** November 15, 2025
**Analysis Type:** Central Ledger System Review
**Repository:** TowsifAhamed/FinTechFrauds

---

## Executive Summary

The FinTechFrauds repository implements a **moderated, tamper-evident central ledger** for tracking confirmed fraudulent accounts. This analysis documents the ledger's architecture, capabilities, data flow, and identifies both strengths and gaps in the current implementation.

**Key Finding:** The ledger successfully stores approved fraud reports with hash-chain tamper evidence, but **lacks query/search endpoints** to access the historical ledger data.

---

## 1. Ledger Architecture

### 1.1 Purpose

The central fraud ledger serves as a **shared, tamper-evident database** of confirmed fraudulent transactions across multiple organizations (reporters). It enables:

- **Fraud report submission** from member organizations
- **Human moderation** to verify reports before approval
- **Tamper-evident storage** using cryptographic hash chains
- **Deduplication** to prevent duplicate reports
- **Audit trail** of all approved fraud cases

### 1.2 Components

```
┌─────────────────┐
│ Reporter Orgs   │ (Submit fraud reports via HMAC-signed API)
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ LedgerController (/v1/ledger/report)        │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ LedgerService                               │
│ - Validates & enqueues reports              │
│ - Deduplicates using day-based keys         │
│ - Maintains pending queue                   │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ ModerationController (/v1/ledger/moderate)  │
│ - Analysts review pending reports           │
│ - Approve or reject fraud claims            │
└────────┬────────────────────────────────────┘
         │
         ▼ (on APPROVE)
┌─────────────────────────────────────────────┐
│ Approved Ledger (data/approved-ledger.jsonl)│
│ - Append-only JSONL file                    │
│ - Hash-chained entries                      │
│ - Schema validated                          │
└─────────────────────────────────────────────┘
```

---

## 2. Data Model

### 2.1 Ledger Entry Schema

**File:** `ledger/approved-ledger.schema.json`

Each approved fraud report contains:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | Unique report ID (UUID) |
| `reporter` | string | ✅ | Organization that submitted the report |
| `accountHash` | string | ✅ | **Hashed fraudulent account identifier** |
| `merchantHash` | string/null | ⚠️ | Hashed merchant identifier (if applicable) |
| `descriptionTokensHash` | string | ✅ | Hash of transaction description tokens |
| `description` | string | ✅ | Normalized transaction description |
| `amountCents` | integer | ✅ | Transaction amount in cents (≥0) |
| `countryCode` | string/null | ⚠️ | 3-letter country code |
| `reportedAt` | integer/null | ⚠️ | Original fraud timestamp (epoch ms) |
| `status` | string | ✅ | Always "APPROVED" in this file |
| `moderatedAt` | string | ✅ | ISO 8601 timestamp of approval |
| `moderator` | string | ✅ | Analyst who approved the report |
| `version` | integer | ✅ | Schema version (currently 1) |
| `prevHash` | string/null | ✅ | Previous entry's hash (null for first) |
| `hash` | string | ✅ | SHA-256 hash of this entry |

**Example Entry:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "reporter": "risk_ops_team_alpha",
  "accountHash": "acct_7f8a9b2c...",
  "merchantHash": "merch_4d3e2f1a...",
  "descriptionTokensHash": "tokens_abc123...",
  "description": "STORED_VALUE_PROVIDER",
  "amountCents": 15600,
  "countryCode": "US",
  "reportedAt": 1722506400000,
  "status": "APPROVED",
  "moderatedAt": "2025-11-15T12:30:45Z",
  "moderator": "analyst_jane_doe",
  "version": 1,
  "prevHash": "dGVzdGhhc2g...",
  "hash": "bXlhY3R1YWxo..."
}
```

### 2.2 Key Fields for Fraud Detection

**Most Important:** `accountHash` - This is the **hashed identifier of the fraudulent account**

- All transactions from accounts in this ledger are **confirmed fraud**
- Other systems can query this ledger to check if an account has a fraud history
- Hashed for privacy (original account ID never exposed)

---

## 3. Current API Endpoints

### 3.1 Submit Fraud Report

**Endpoint:** `POST /v1/ledger/report`

**Implementation:** `LedgerController.java` (line 29-39)

**Authentication:** HMAC-SHA256 required

**Request Body:**
```json
{
  "reporter": "org_hash_demo",
  "accountHash": "acct_hash_demo",
  "merchantHash": "m_demo",
  "description": "STORED_VALUE_PROVIDER",
  "descriptionTokensHash": "t_giftcards",
  "amountCents": 8800,
  "reportedAt": 1700000000000
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "queuedAt": "2025-11-15T12:30:45Z"
}
```

**Features:**
- ✅ HMAC signature verification
- ✅ Automatic deduplication (same reporter + merchant + description + day)
- ✅ Returns 409 Conflict if already approved
- ✅ Idempotency key support
- ✅ Rate limiting enforced

### 3.2 Get Pending Count

**Endpoint:** `GET /v1/ledger/pending/count`

**Implementation:** `LedgerController.java` (line 41-44)

**Response:**
```json
{
  "count": 5
}
```

**Use Case:** Monitor moderation queue depth

### 3.3 Peek Next Pending Report

**Endpoint:** `GET /v1/ledger/pending/next`

**Implementation:** `LedgerController.java` (line 46-61)

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "receivedAt": "2025-11-15T10:15:30Z",
  "payload": {
    "reporter": "org_hash_demo",
    "accountHash": "acct_hash_demo",
    "merchantHash": "m_demo",
    "description": "STORED_VALUE_PROVIDER",
    "descriptionTokensHash": "t_giftcards",
    "amountCents": 8800
  }
}
```

**Use Case:** Analyst reviews report details before moderating

### 3.4 Moderate Report

**Endpoint:** `POST /v1/ledger/moderate`

**Implementation:** `ModerationController.java` (line 26-35)

**Request:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "action": "APPROVE",
  "moderator": "analyst_jane_doe"
}
```

**Actions:**
- `APPROVE` - Append to ledger file with hash chain
- `REJECT` - Remove from queue without recording

**Response (APPROVE):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "APPROVE"
}
```

---

## 4. Ledger Service Implementation

### 4.1 Hash Chain Tamper Evidence

**Implementation:** `LedgerService.java` (line 147-205)

**Algorithm:**
```
hash = SHA256(prevHash + canonical_json)
```

Where `canonical_json` is the entry with:
- Sorted keys (alphabetically)
- prevHash field included
- hash field excluded from calculation

**Verification Process:**
1. Read entry from JSONL file
2. Extract `hash` and `prevHash` fields
3. Remove `hash` field from entry
4. Serialize to canonical JSON (sorted keys)
5. Compute: `expected = SHA256(prevHash + canonical_json)`
6. Verify: `expected == hash`

**First Entry:**
- `prevHash` is `null`
- `hash = SHA256("" + canonical_json)`

**Subsequent Entries:**
- `prevHash` = previous entry's `hash`
- Creates unbreakable chain

**Tamper Detection:**
- Any modification to ANY field breaks the hash chain
- Modification of entry N invalidates all hashes from N+1 onwards
- Deletion of entries is detectable (hash mismatch)

### 4.2 Deduplication Strategy

**Implementation:** `LedgerService.java` (line 285-315)

**Dedupe Key Format:**
```
reporter|merchantHash|descriptionTokensHash|day
```

**Example:**
```
risk_ops_alpha|merch_abc123|tokens_xyz789|2025-11-15
```

**Deduplication Levels:**

1. **In-Memory Queue** (line 72-87)
   - Prevents duplicate pending reports
   - Same dedupe key returns existing pending report

2. **Approved Ledger** (line 61-71)
   - Checks if report already approved for same day
   - Returns 409 Conflict if duplicate
   - Loads dedupe keys on service initialization

**Rationale:**
- Same organization + same merchant + same fraud pattern + same day = likely duplicate
- Allows same fraud pattern on different days (tracking ongoing fraud)

### 4.3 Storage

**File Location:** `serve/data/approved-ledger.jsonl`

**Format:** JSON Lines (JSONL)
- One JSON object per line
- Newline-separated
- Append-only (never modify existing lines)

**Initialization:** `LedgerService.java` (line 257-274)
- On startup, reads entire file
- Extracts last hash for chain continuation
- Builds dedupe key set for duplicate detection

**Concurrency:**
- `synchronized` keyword on `moderate()` method (line 113)
- Prevents concurrent writes
- Thread-safe in-memory structures (ConcurrentHashMap, ConcurrentLinkedQueue)

---

## 5. Security Features

### 5.1 Authentication

**All ledger endpoints require HMAC-SHA256 authentication** except in dev mode.

**Headers Required:**
- `X-Api-Key`: API key identifier
- `X-Timestamp`: ISO 8601 timestamp
- `X-Nonce`: Unique nonce (prevents replay)
- `X-Signature`: HMAC-SHA256 signature
- `X-Idempotency-Key`: Prevents duplicate requests

**Signature Calculation:**
```
canonical = timestamp + "\n" + nonce + "\n" + SHA256(body)
signature = Base64(HMAC-SHA256(secret, canonical))
```

**Timestamp Validation:**
- Request must be within ±5 minutes of server time
- Prevents replay attacks

### 5.2 Privacy (PII Hashing)

**All identifiers are hashed client-side:**
- `accountHash` - Original account ID never sent
- `merchantHash` - Original merchant ID never sent
- `descriptionTokensHash` - Original tokens never sent

**Hash Algorithm:** SHA-256 with tenant-specific salts

**Benefits:**
- Ledger operators cannot reverse-engineer account IDs
- Cross-organization fraud detection without exposing PII
- GDPR/CCPA compliant

### 5.3 Integrity Validation

**JSON Schema Validation:** (line 249-255)
- Every entry validated against `approved-ledger.schema.json`
- Enforces required fields
- Type checking
- Prevents malformed entries

**Hash Chain Validation:**
- Test suite validates hash chain integrity (LedgerFlowTest.java line 114-127)
- Production systems can verify entire chain on load

---

## 6. Workflow Example

### Full Lifecycle: Report → Moderate → Ledger

**Step 1: Organization Detects Fraud**
```bash
# Org "risk_ops_alpha" detects gift card fraud
# accountHash: acct_7f8a9b2c (fraudulent account)
```

**Step 2: Submit Fraud Report**
```bash
curl -X POST http://localhost:8080/v1/ledger/report \
  -H "X-Api-Key: demo_key" \
  -H "X-Timestamp: 2025-11-15T12:00:00Z" \
  -H "X-Nonce: abc-123-def" \
  -H "X-Signature: <hmac>" \
  -H "X-Idempotency-Key: unique-key-123" \
  -d '{
    "reporter": "risk_ops_alpha",
    "accountHash": "acct_7f8a9b2c",
    "merchantHash": "merch_giftcard",
    "description": "STORED_VALUE_PROVIDER",
    "descriptionTokensHash": "tokens_giftcard",
    "amountCents": 15600,
    "reportedAt": 1722506400000
  }'

# Response: {"id": "550e8400...", "queuedAt": "2025-11-15T12:00:00Z"}
```

**Step 3: Analyst Reviews**
```bash
curl -X GET http://localhost:8080/v1/ledger/pending/next \
  -H "X-Api-Key: demo_key" \
  -H "X-Timestamp: 2025-11-15T12:05:00Z" \
  -H "X-Nonce: xyz-456-ghi" \
  -H "X-Signature: <hmac>"

# Response: Full report with payload for review
```

**Step 4: Analyst Approves**
```bash
curl -X POST http://localhost:8080/v1/ledger/moderate \
  -H "X-Api-Key: demo_key" \
  -H "X-Timestamp: 2025-11-15T12:10:00Z" \
  -H "X-Nonce: mno-789-pqr" \
  -H "X-Signature: <hmac>" \
  -d '{
    "id": "550e8400...",
    "action": "APPROVE",
    "moderator": "analyst_jane_doe"
  }'

# Response: {"id": "550e8400...", "status": "APPROVE"}
```

**Step 5: Entry Appended to Ledger**
```jsonl
{"id":"550e8400...","reporter":"risk_ops_alpha","accountHash":"acct_7f8a9b2c","merchantHash":"merch_giftcard","descriptionTokensHash":"tokens_giftcard","description":"STORED_VALUE_PROVIDER","amountCents":15600,"countryCode":"US","reportedAt":1722506400000,"status":"APPROVED","moderatedAt":"2025-11-15T12:10:00Z","moderator":"analyst_jane_doe","version":1,"prevHash":"dGVz...","hash":"bXlh..."}
```

**Step 6: Fraud Detection Systems Query Ledger**
```
⚠️ MISSING - No query endpoint exists (see Section 7)
```

---

## 7. Current Gaps and Limitations

### 7.1 ❌ No Query/Search Endpoints

**Critical Gap:** The ledger stores approved fraud reports but provides **no API to read or query them**.

**Missing Endpoints:**

1. **GET /v1/ledger/entries**
   - List all approved ledger entries (paginated)
   - Filter by date range
   - Filter by reporter

2. **GET /v1/ledger/account/{accountHash}**
   - Check if an account is in the fraud ledger
   - Return all fraud reports for that account
   - **Critical for real-time fraud prevention**

3. **GET /v1/ledger/merchant/{merchantHash}**
   - Check if a merchant has fraud reports
   - Identify compromised merchants

4. **GET /v1/ledger/search**
   - Search by multiple criteria
   - Filter by amount range
   - Filter by description pattern

5. **GET /v1/ledger/stats**
   - Aggregate statistics
   - Fraud trends over time
   - Most common fraud patterns

**Impact:**
- Organizations cannot **check if an account is in the ledger** during transaction scoring
- No way to **download the full ledger** for offline analysis
- **Limited utility** - ledger is write-only

**Workaround:**
- Direct file access to `serve/data/approved-ledger.jsonl`
- Parse JSONL manually
- Build custom indexing layer

### 7.2 ⚠️ In-Memory Dedupe Index

**Current Implementation:** `LedgerService.java` (line 47)
```java
private final Map<String, String> dedupeIndex = new ConcurrentHashMap<>();
private final java.util.Set<String> approvedDedupeKeys = ConcurrentHashMap.newKeySet();
```

**Issue:**
- Dedupe keys loaded into memory on startup (line 257-274)
- Large ledgers (millions of entries) may cause OOM errors
- No persistent index

**Better Approach:**
- Use Redis for dedupe index
- Use database with proper indexing
- Use RocksDB for embedded key-value storage

### 7.3 ⚠️ File-Based Storage Scalability

**Current Storage:** Append-only JSONL file

**Limitations:**
- **Linear scan** required to find entries
- No indexing on accountHash, merchantHash, etc.
- Large files (>1GB) slow to load
- No partitioning by date/region

**Better Approach:**
- Migrate to PostgreSQL with indexes on accountHash, merchantHash
- Use Parquet for columnar storage (analytics)
- Partition by date for better query performance

### 7.4 ⚠️ No Bulk Import

**Missing Feature:** Bulk import of historical fraud data

**Use Case:**
- Migrating from legacy systems
- Loading industry fraud databases
- Backfilling from CSV/Excel

**Current:** Must submit one report at a time via API

### 7.5 ⚠️ No Ledger Verification Tool

**Missing:** Command-line tool to verify ledger integrity

**Should Include:**
- Verify entire hash chain
- Validate all entries against schema
- Detect missing entries (gaps in chain)
- Report statistics

**Current:** Manual verification only in tests

---

## 8. Fraud Detection Use Cases

### 8.1 Real-Time Account Lookup ❌ (Not Implemented)

**Scenario:** During transaction scoring, check if account is known fraudster

**Ideal Flow:**
```
1. Transaction arrives: accountHash = "acct_7f8a9b2c"
2. Query: GET /v1/ledger/account/acct_7f8a9b2c
3. Response: {"inLedger": true, "reportCount": 3, "firstSeen": "2025-10-01"}
4. Decision: Auto-decline if in ledger
```

**Current Status:** ❌ **Cannot be implemented** - no query endpoint

**Workaround:**
- Pre-load entire ledger into Redis
- Build custom index of accountHash → report count
- Refresh periodically

### 8.2 Merchant Risk Scoring ❌ (Not Implemented)

**Scenario:** Flag high-risk merchants with multiple fraud reports

**Ideal Flow:**
```
1. Transaction at merchantHash = "merch_giftcard"
2. Query: GET /v1/ledger/merchant/merch_giftcard
3. Response: {"reportCount": 47, "totalAmountCents": 850000}
4. Decision: Apply extra scrutiny or decline
```

**Current Status:** ❌ **Cannot be implemented** - no query endpoint

### 8.3 Fraud Pattern Analysis ❌ (Limited)

**Scenario:** Identify trending fraud patterns (e.g., gift card scams)

**Ideal Flow:**
```
1. Query: GET /v1/ledger/stats?groupBy=description&since=2025-11-01
2. Response: {
     "STORED_VALUE_PROVIDER": 145,
     "ELECTRONICS_SUPERSTORE": 23,
     "FURNITURE_OUTLET_NEW": 12
   }
3. Action: Update rules engine to flag gift card transactions
```

**Current Status:** ❌ **Cannot be implemented** - no analytics endpoint

**Workaround:**
- Export JSONL file
- Load into analytics tool (Jupyter, pandas, SQL)
- Manual analysis

---

## 9. Test Coverage

### 9.1 LedgerFlowTest.java

**Test: Report-Moderate-Approve Flow** (line 75-128)
- ✅ Submit fraud report with HMAC auth
- ✅ Verify report queued (status 202)
- ✅ Check pending count > 0
- ✅ Moderate report (APPROVE action)
- ✅ Verify ledger file created
- ✅ Validate JSON schema compliance
- ✅ **Verify hash chain integrity**

**Test: Duplicate Detection** (line 131-179)
- ✅ Submit fraud report
- ✅ Moderate and approve
- ✅ Attempt duplicate submission
- ✅ Verify 409 Conflict response

**Hash Chain Validation Code:**
```java
ObjectNode tree = (ObjectNode) objectMapper.readTree(line);
String prevHash = tree.path("prevHash").isNull() ? null : tree.path("prevHash").asText(null);
tree.remove("hash");
String canonical = canonicalMapper.writeValueAsString(tree);
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] expected = digest.digest(((prevHash == null ? "" : prevHash) + canonical).getBytes(UTF_8));
assertThat(objectMapper.readTree(line).path("hash").asText())
    .isEqualTo(Base64.getEncoder().encodeToString(expected));
```

### 9.2 LedgerServiceFailureTest.java

**Tests error handling scenarios**

---

## 10. Recommendations

### 10.1 High Priority: Add Query Endpoints

**Implement the following endpoints:**

```java
// 1. Check if account is in ledger (real-time fraud prevention)
@GetMapping("/account/{accountHash}")
public ResponseEntity<AccountLedgerInfo> getAccountInfo(@PathVariable String accountHash);

// 2. List all entries (paginated)
@GetMapping("/entries")
public ResponseEntity<Page<LedgerEntry>> listEntries(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "100") int size,
    @RequestParam(required = false) String since,
    @RequestParam(required = false) String until
);

// 3. Search entries
@GetMapping("/search")
public ResponseEntity<Page<LedgerEntry>> search(
    @RequestParam(required = false) String accountHash,
    @RequestParam(required = false) String merchantHash,
    @RequestParam(required = false) String reporter
);

// 4. Aggregate statistics
@GetMapping("/stats")
public ResponseEntity<LedgerStats> getStats(
    @RequestParam(required = false) String since,
    @RequestParam(required = false) String groupBy
);
```

### 10.2 Medium Priority: Improve Storage

**Migrate to database:**
- Use PostgreSQL with indexes on accountHash, merchantHash
- Keep JSONL as backup/archive
- Maintain hash chain in database

**Schema:**
```sql
CREATE TABLE approved_fraud_ledger (
    id UUID PRIMARY KEY,
    reporter VARCHAR NOT NULL,
    account_hash VARCHAR NOT NULL,  -- INDEX
    merchant_hash VARCHAR,           -- INDEX
    description_tokens_hash VARCHAR,
    description VARCHAR,
    amount_cents BIGINT,
    country_code VARCHAR(3),
    reported_at TIMESTAMPTZ,
    status VARCHAR DEFAULT 'APPROVED',
    moderated_at TIMESTAMPTZ NOT NULL,
    moderator VARCHAR NOT NULL,
    version INT DEFAULT 1,
    prev_hash VARCHAR,
    hash VARCHAR NOT NULL,             -- INDEX
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_account_hash ON approved_fraud_ledger(account_hash);
CREATE INDEX idx_merchant_hash ON approved_fraud_ledger(merchant_hash);
CREATE INDEX idx_moderated_at ON approved_fraud_ledger(moderated_at);
```

### 10.3 Low Priority: Operational Tools

**1. Ledger Verification Tool**
```bash
java -jar ledger-verifier.jar --file=data/approved-ledger.jsonl
# Output: ✅ Chain verified (1,234 entries, no errors)
```

**2. Bulk Import Tool**
```bash
java -jar ledger-import.jar --csv=historical-fraud.csv --moderator=migration_bot
```

**3. Export Tool**
```bash
java -jar ledger-export.jar --since=2025-11-01 --format=csv > november-fraud.csv
```

---

## 11. Conclusion

### 11.1 Summary

The FinTechFrauds ledger system demonstrates **solid foundational architecture**:

**Strengths:**
- ✅ Tamper-evident hash chain (cryptographically secure)
- ✅ Human moderation workflow (prevents false positives)
- ✅ HMAC authentication (secure submission)
- ✅ Deduplication (prevents spam)
- ✅ JSON schema validation (data quality)
- ✅ Comprehensive test coverage

**Critical Gap:**
- ❌ **No query/search endpoints** - ledger is write-only
- Cannot check if an account is in the fraud ledger during scoring
- Limited operational utility without query capability

**Recommendations:**
1. **Immediate:** Implement `GET /v1/ledger/account/{accountHash}` for real-time lookup
2. **Short-term:** Add list/search/stats endpoints
3. **Long-term:** Migrate to PostgreSQL for scalability and indexing

### 11.2 Answer to Original Question

**"What about the central ledger of accounts that do scam?"**

**Current Status:**
- ✅ The ledger **exists** and **works correctly**
- ✅ Fraud reports are stored with **accountHash** (the fraudulent account identifier)
- ✅ Entries are **tamper-evident** via hash chain
- ✅ Duplicate prevention ensures data quality
- ❌ **BUT: No API to query the ledger**

**The ledger tracks fraudulent accounts but doesn't expose them for consumption.**

**File Location:** `serve/data/approved-ledger.jsonl`

**Example Entry:**
```json
{
  "accountHash": "acct_gamma_masked",  ← This is the fraudulent account
  "merchantHash": "52221ba04ee4ca97",
  "description": "STORED_VALUE_PROVIDER",
  "amountCents": 1560,
  "status": "APPROVED",
  "moderatedAt": "2025-11-15T12:30:45Z"
}
```

**To use this data today:**
- Read JSONL file directly
- Parse and index manually
- Build custom query layer

**Ideal future state:**
- Query API: `GET /v1/ledger/account/acct_gamma_masked`
- Response: `{"inLedger": true, "reportCount": 12}`
- Integrate into real-time scoring pipeline

---

**Document Version:** 1.0
**Last Updated:** 2025-11-15
**Analysis Type:** Detailed architectural review
