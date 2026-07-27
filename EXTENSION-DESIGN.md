# Extension Design

## Extension A: Rate Limiting & Fan-Out Control

### Current State

The poller fires every 60 seconds on a fixed interval and makes a single unauthenticated request to the GitHub public events API. There is no secondary fan-out (no enrichment calls to actor/repo URLs). The unauthenticated rate limit is 60 requests per hour.

### What We're Not Handling

**ETag / Conditional Requests**

The GitHub Events API is optimized for polling with the `ETag` header. Each response includes an `ETag` value, and sending `If-None-Match` on subsequent requests returns `304 Not Modified` when no new events exist — consuming zero rate limit budget. We currently ignore this entirely, meaning every poll costs a request even when nothing has changed.

**X-Poll-Interval Compliance**

GitHub returns an `X-Poll-Interval` header indicating the minimum seconds between polls. Under high server load this value increases beyond 60s. We hardcode 60s and don't respect this dynamic backoff signal.

**Rate Limit Exhaustion Handling**

When rate-limited (HTTP 403 with `X-RateLimit-Remaining: 0`), we log the error and retry in 60s — burning retries against a wall. We don't read `X-RateLimit-Reset` to determine when we can resume.

### Future Enhancements

1. **Store the ETag between polls.** After each successful response, persist the `ETag` header value in memory. On the next poll, send `If-None-Match: "<etag>"`. If the response is `304`, skip processing entirely.

2. **Respect X-Poll-Interval.** Read this header from each response and use it as the minimum delay before the next poll. If the value exceeds our configured 60s, honor it.

3. **Back off on rate limit exhaustion.** On a 403/429 response, read `X-RateLimit-Reset` (Unix epoch timestamp) and calculate how long to sleep. Skip polling until that time passes rather than retrying blindly.

4. **Fan-out control for enrichment.** If we later add calls to actor/repo detail URLs, we would need:
    - A cache layer (the `existsByActorId` / `existsByRepositoryId` checks already prevent redundant enrichment fetches)
    - Throttled batching to avoid amplifying 30 events into 60 additional API calls per cycle

### Tradeoffs

- ETag support adds a small amount of state management but dramatically reduces wasted requests. For a single-instance deployment, an in-memory field is sufficient. For multi-instance, you'd need to store it externally (Redis, DB).
- Respecting X-Poll-Interval means we may poll less frequently than 60s during high load periods, potentially missing some events in the 300-event timeline window. This is acceptable because the API itself is throttling us — pushing harder would just result in errors.
- The current fixed 60s interval is simple and predictable. Dynamic intervals add complexity but align with GitHub's documented best practices.

---

## Extension B: Idempotency & Restart Safety

### Current State

**Idempotency** is handled via:

- A `UNIQUE` constraint on `event_id` in the `push_events` table
- An `existsByEventId` check before insert (fast path to avoid unnecessary write attempts)
- A `DataIntegrityViolationException` catch to handle race conditions where the event was inserted between the exists check and the save

**Restart safety** is inherent in the design:

- The poller uses `@Scheduled(fixedDelay = 60)`, meaning it waits 60s after the previous poll _completes_ — no overlapping executions
- Each event is processed independently; a failure on one event doesn't roll back others
- On restart, the service simply resumes polling; duplicates are rejected by the unique constraint

### What We're Not Handling: Unbounded Growth

The GitHub public events API can return up to 300 events per page. Polling every 60 seconds, the `push_events` table (particularly the JSONB `raw_payload` column) grows indefinitely. In a production deployment this would eventually degrade query performance and exhaust disk.

### Future Enhancements

1. **Retention policy via scheduled cleanup.** A `@Scheduled` task that runs daily and deletes rows older than a configurable retention period (e.g., 30 days). Since UUIDv7 encodes a timestamp, we can derive age from the primary key without needing a separate `created_at` column. Alternatively, a simple `created_at` column makes the query more explicit.

2. **Table partitioning.** For high-volume production use, partition `push_events` by time range (e.g., weekly). Old partitions can be dropped cheaply with `DROP TABLE` rather than row-by-row deletion, which avoids vacuum pressure.

3. **Archival to cold storage.** Move raw payloads older than N days to object storage (S3) and retain only the structured fields in the hot table. This keeps the primary table lean for queries while preserving audit data.

### Corruption Prevention

- **Partial writes:** Actor and repository persistence happen before the push event insert. If the push event insert fails, the actor/repo rows remain — these are harmless (they're reference data that would be needed by the next event from the same actor/repo anyway).
- **Concurrent polls:** The `fixedDelay` scheduler guarantees no overlap. If we moved to a multi-instance deployment, the unique constraint on `event_id` ensures at-most-once semantics at the database level.
- **Schema drift:** Liquibase manages the schema, and `spring.jpa.hibernate.ddl-auto=validate` ensures Hibernate refuses to start if the schema doesn't match the entity model.

### Tradeoffs

- A retention policy means we lose historical data. For this use case (event ingestion from a public API with a 30-day server-side window), this is acceptable — the data is ephemeral by nature.
- Partitioning adds operational complexity (partition management, query planning awareness) but pays for itself at scale.
- Not implementing a retention policy is acceptable for short-lived evaluation environments where disk is not a concern.

---

## Extension C: Object Storage

### Overview

Contributor avatar images are stored in AWS S3 to provide durable, independently-accessible copies that don't depend on GitHub CDN availability. The S3 object key is persisted on the `actors` table as a reference, tying the stored asset's lifecycle directly to the actor record.

### Design

1. **Download on first encounter.** When a new actor is persisted, the service downloads the avatar from `avatar_url` and uploads it to an S3 bucket under a deterministic key (e.g., `avatars/{actor_id}.png`).

2. **Persist the S3 key on the actor table.** A new nullable column `avatar_s3_key` on the `actors` table stores the object key. This is the durable reference — consumers use it to retrieve the image from S3 rather than hitting GitHub's CDN.

3. **Avoid unnecessary re-downloads.** The `existsByActorId` check already prevents re-processing known actors. If the actor row exists and `avatar_s3_key` is populated, the avatar is already stored — no download or upload occurs. This gives us the same deduplication guarantee we use for event ingestion.

4. **Bounded storage via TTL alignment.** The S3 bucket is configured with a lifecycle rule that matches the actor retention policy. When actor rows are purged (per Extension B's retention policy), their corresponding S3 objects expire via the same TTL window. This can be implemented as:
    - An S3 lifecycle rule that deletes objects older than N days (matching the DB retention period)
    - Or, a cleanup task that deletes S3 objects when their corresponding actor rows are deleted

    Either approach ensures storage doesn't grow unbounded.

### Tradeoffs

- **Network cost at ingestion time.** Downloading and re-uploading avatars adds latency to the ingestion path. This could be moved to an async background task (e.g., a separate `@Scheduled` job or event-driven via a message queue) to avoid slowing down the poller.
- **S3 lifecycle rules vs. application-managed deletion.** Lifecycle rules are operationally simpler (no application code needed for cleanup) but less precise — they operate on object creation time, not on whether the actor row still exists. Application-managed deletion is more accurate but adds code complexity.
- **Nullable column for gradual backfill.** Making `avatar_s3_key` nullable means existing actors without stored avatars are still valid. A backfill job could populate missing entries on a best-effort basis without blocking normal operation.
- **Cost.** Avatar images are small (typically <100KB). Even at scale, storage costs are negligible. The primary concern is request volume against GitHub's CDN during initial ingestion bursts, which the exists-check naturally throttles.

---

## Extension D: Testing Strategy

### Approach

Tests are split into two tiers: fast unit tests that validate logic in isolation, and an integration test that proves the system works end-to-end against a real database.

### Unit Tests

**What:** Controller, service, and specification utility logic tested with Mockito mocks — no Spring context, no database.

- `PushEventControllerTest` — Uses `@WebMvcTest` to verify HTTP binding, response structure, and parameter handling without starting the full application.
- `PushEventServiceTest` — Verifies the service delegates to the repository with a composed specification. Mocks the repository layer.
- `PushEventSpecificationUtilsTest` — Verifies each specification method returns null when the parameter is null (no predicate) and returns a proper equality predicate when present.

**Why these:** The controller and service are thin layers, but testing them confirms Spring wires request parameters correctly and the specification composition works as expected. The spec utils are pure functions with clear null/non-null behavior — ideal for unit testing.

### Integration Test

**What:** `GitHubEventsPollerIntegrationTest` boots the full Spring context against a real PostgreSQL instance (Testcontainers) with the GitHub API stubbed via WireMock.

**Scenarios covered:**

- Ingests push events and persists actor/repository data to all three tables
- Filters out non-PushEvent types (e.g., WatchEvent)
- Deduplicates on repeated polls (count doesn't increase)
- Handles HTTP 500 from the API without crashing
- Handles malformed JSON without crashing

**Why this approach:** The poller is the core of the application — it coordinates HTTP calls, JSON deserialization, deduplication, and multi-table persistence. Mocking the API (WireMock) while using a real database (Testcontainers) gives high confidence that the full pipeline works without depending on GitHub's availability or rate limits.

### What We Chose Not to Test

- **Repository layer** — Spring Data JPA generates the implementation; testing `existsByEventId` would just be testing the framework.
- **Liquibase migrations** — Validated implicitly by the integration test (Hibernate's `ddl-auto=validate` would fail if the schema didn't match).
- **Actor/repository persistence in isolation** — Covered by the integration test's assertions on `existsByActorId` and `existsByRepositoryId`.
