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

V1 is intentionally wrong: for every declaration it performs three full scans of the input list, and each scan executes a database query. That means the total query count is:

```text
3 * n * n
```

For a single declaration this is still at least three database calls. For larger batches the database calls grow quadratically.

V2 keeps the same response contract but loads only the requested reference data in 1000-record batches:

```text
3 database queries per 1000-record batch
```

Each batch uses `where ... in (...)` queries for sender references, receiver references, and goods category/risk pairs. The CPU work is still a simple input scan, but the database layer no longer performs per-record lookups.

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

Default tests exclude the largest V1 cases because they are intentionally brutal against real PostgreSQL. To include them:

```bash
./gradlew test -DincludeSlowTests=true
```

The V1 5000 item test performs:

```text
3 * 5000 * 5000 = 75,000,000 database queries
```

The V2 5000 item test performs only 15 database queries.
