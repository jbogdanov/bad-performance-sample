# Bad Database Performance Sample

This Spring Boot project demonstrates a deliberately inefficient database-layer pattern for Big O discussions, then compares it with an optimized version.

The sample uses customs goods declarations as input data. Each declaration references:

- sender
- receiver
- goods category
- risk assessment

## Project Layout

```text
com.bogdanov.performance
├── common
│   ├── io
│   └── model
├── database
├── v1
└── v2
```

V1 is intentionally naive: for every declaration it performs three database lookups, then runs a second loop over the whole input file for split-consignment detection. That means the work is:

```text
3 * n database queries
n * n split-consignment rule database queries
```

For a single declaration this is still at least three database calls. For larger batches the business rule grows quadratically.

The second loop exists for a real customs/tax scenario: split-consignment detection. If multiple declarations in the same input file share the same sender, receiver, goods category, and origin country, V1 totals their combined declared value by scanning the whole input file for each declaration. Every comparison asks the database for the relevant split-consignment rule by goods category and origin country. When the combined value is above the rule's manual-review threshold, those records are flagged as `manualReviewRecords`.

V2 keeps the same response contract but loads only the requested reference data in 1000-record batches:

```text
3 database queries per 1000-record batch
1 split-consignment rule query per request
```

Each batch uses `where ... in (...)` queries for sender references, receiver references, and goods category/risk pairs. The CPU work is still a simple input scan, but the database layer no longer performs per-record lookups.

V2 handles split-consignment detection with grouping instead of a nested scan, and loads only the split-consignment rules referenced by the input list.

## Local PostgreSQL

The app now uses PostgreSQL instead of H2. Start a local database with Docker:

```bash
docker compose up -d
```

Default connection settings:

```text
DB_URL=jdbc:postgresql://localhost:15432/customs
DB_USERNAME=customs
DB_PASSWORD=customs
```

## REST API

```bash
./gradlew bootRun
```

Post declarations to either version:

```text
POST /api/v1/declarations/process
POST /api/v2/declarations/process
```

The legacy V1 alias still works:

```text
POST /api/declarations/process
```

The request body is a JSON array of goods declarations:

```json
[
  {
    "declarationId": "DEC-00001",
    "senderReference": "SND-0018",
    "receiverReference": "RCV-0030",
    "goodsCategoryCode": "CAT-008",
    "riskAssessmentCode": "RISK-06",
    "originCountry": "EE",
    "declaredValue": 137.01,
    "weightKg": 2.3
  }
]
```

The response contains timing and query-count metrics:

```json
{
  "records": 1,
  "validRecords": 1,
  "manualReviewRecords": 0,
  "databaseQueries": 3,
  "totalMillis": 5.0,
  "averageMillisPerRecord": 5.0
}
```

## Tests

The MockMvc integration tests use Testcontainers with PostgreSQL.

```bash
./gradlew test
```

The V2 5000 item test performs only 16 database queries.
