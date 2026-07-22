# Design Brief: GitHub Push Event Ingestion Service

## Problem Understanding

The prompt asks for an internal service that ingests GitHub Push events, enriches them with related data, and stores them for querying and analysis. Reading between the lines, the core challenge isn't the ingestion itself—it's doing it responsibly under tight constraints: no auth token (60 req/hr), the need to run unattended without crash-looping, and making the data actually useful for future consumers.

I interpreted this as: build a polling pipeline that continuously pulls from the GitHub Public Events API, filters for `PushEvent` types, enriches them with actor/repository metadata, and persists everything durably in PostgreSQL. The system should be boring in production—predictable, observable, and graceful under failure.

## Proposed Architecture

**Runtime:** Java 25, Spring Boot 4, PostgreSQL, Docker Compose  
**Why not Rails:** Java/Spring Boot is my strongest stack. The architecture and design thinking are the same regardless of framework—the language is incidental to the system design.

### Components

The application follows a standard Spring Boot layered architecture:

- **Web (Controllers)** — REST endpoints for querying persisted events. Handles request validation, pagination, and response serialization.
- **Service** — Business logic layer. The ingestion service orchestrates polling, filtering, deduplication, and enrichment. The query service translates API parameters into repository calls.
- **Repository (JPA)** — Data access layer. Spring Data JPA repositories handle persistence and provide derived query methods for structured field lookups.

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot Application               │
│                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │  Scheduled   │───▶│   Ingestion  │───▶│   JPA     │ │
│  │  Poller      │    │   Service    │    │   Repos   │ │
│  └──────────────┘    └──────────────┘    └───────────┘ │
│         │                    │                   │       │
│         ▼                    ▼                   ▼       │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │  Rate Limit  │    │  Enrichment  │    │ PostgreSQL│ │
│  │  State (ETag │    │  Service     │    │           │ │
│  │  + Interval) │    │  (Actor/Repo)│    │           │ │
│  └──────────────┘    └──────────────┘    └───────────┘ │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │              REST API (Query Layer)               │   │
│  │   GET /events, GET /events/{id}, GET /events?... │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Data Model

**Phase 1 — `push_events` table:**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | Internal PK |
| event_id | VARCHAR(unique) | GitHub event ID, dedupe key |
| raw_payload | JSONB | Full event JSON for audit |
| created_at | TIMESTAMP | Ingestion time |

**Phase 2 — Add structured query columns:**
| Column | Type | Notes |
|--------|------|-------|
| actor_id | BIGINT | FK to `actors`, from `actor.id` |
| repository_id | BIGINT | FK to `repositories`, from `payload.repository_id` |
| push_id | BIGINT | From `payload.push_id` |
| ref | VARCHAR | From `payload.ref` |
| head | VARCHAR(40) | From `payload.head` |
| before | VARCHAR(40) | From `payload.before` |
| event_created_at | TIMESTAMP | From `created_at` on the event |

**Phase 3 — Enrichment tables:**

`actors` (id, login, display_login, avatar_url, url)  
`repositories` (id, name, url)

Linked via foreign keys on `push_events.actor_id` and `push_events.repository_id`. Insert-if-not-exists semantics—actor/repo records are written once and not updated on subsequent encounters since profile details change infrequently and enrichment fetches are expensive under rate limits.

### Polling & Rate Limit Strategy

The GitHub Events API provides two headers that govern polling behavior:

- **`ETag`** — A hash of the current event set. On subsequent requests, we send `If-None-Match: "<etag>"`. If events haven't changed, GitHub returns `304 Not Modified` and does not count the request against rate limits.
- **`X-Poll-Interval`** — The minimum number of seconds between polls (typically 60s). We obey this as our scheduling floor.

**Implementation:**

1. A `@Scheduled` poller runs on a configurable interval (default: 60s, respecting `X-Poll-Interval`).
2. The ETag from each successful response is persisted in memory (or a lightweight DB row) and sent with the next request.
3. On `304`, we log it and sleep until the next interval—no processing, no rate cost.
4. On `403` (rate limited), we back off using the `X-RateLimit-Reset` header timestamp and log a warning.
5. On `503` or network errors, we apply exponential backoff with a cap (max ~5 minutes).

This approach means under steady-state we consume at most 1 request per interval, and when events are unchanged, zero effective rate-limit cost.

### Durability & Idempotency

- Events are deduplicated by `event_id` (unique constraint). If the poller restarts or re-fetches overlapping pages, duplicates are silently ignored via `ON CONFLICT DO NOTHING` semantics.
- The ETag is the first line of defense—avoiding re-processing unchanged data entirely.
- Liquibase manages schema migrations, ensuring repeatable and versioned DDL changes.

### Observability

Structured logging (SLF4J + Logback) to stdout/stderr for Docker log aggregation:

- `INFO` — Poll cycle started, events fetched count, events persisted count, 304 no-change
- `WARN` — Rate limit approaching, backoff triggered, enrichment skipped
- `ERROR` — Unrecoverable parse failures, connection errors (with event context for debugging)

Malformed events are logged and skipped rather than crashing the ingestion loop.

## Key Tradeoffs and Assumptions

| Decision                              | Tradeoff                                                                                                                 |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| JSONB raw payload retained            | Storage cost for full audit trail; enables reprocessing if schema evolves                                                |
| Insert-only enrichment (actors/repos) | Stale data possible, but avoids amplifying API calls under tight rate limits                                             |
| Single-threaded poller                | Simpler correctness; sufficient for 60s cadence against one endpoint                                                     |
| No authentication token               | Limited to 60 req/hr; ETag+conditional requests make this viable                                                         |
| Liquibase for migrations              | Explicit versioned DDL; heavier than Flyway but supports rollback                                                        |
| Filtering client-side                 | GitHub public events endpoint does not support server-side type filtering; we discard non-PushEvent in the service layer |

**Assumptions:**

- The GitHub timeline returns at most 300 events per poll with up to 10 pages. In practice, with 60s polling, a single page (up to 100 events with `per_page=100`) captures the recent window without needing pagination.
- Unauthenticated rate limit of 60 req/hr is sufficient when combined with ETag-based conditional requests.
- Events older than 30 days are not available from the API—we accept this as a data boundary.

## What I Intentionally Did Not Build

- **Webhook ingestion** — Would eliminate polling overhead but requires public endpoint exposure and GitHub App configuration; out of scope for a local Docker system.
- **Message queue / async workers** — The volume from a single unauthenticated endpoint doesn't justify the complexity of Kafka/RabbitMQ. A scheduled thread suffices.
- **Object storage for avatars** — Acknowledged in Extension C; would add MinIO to compose and a download-once pipeline. Deferred.
- **Full-text search** — Queryable structured columns satisfy the stated requirements. Elasticsearch would be overkill at this scale.
- **Actor/repo update strategy** — Could track `last_fetched_at` and refresh stale records on a cadence. Deferred to keep enrichment simple and rate-limit-friendly.
- **Authentication** — The prompt explicitly states no token. If added later, rate limits increase to 5000 req/hr, enabling pagination and richer enrichment.
- **Horizontal scaling** — Single instance is correct for this workload. Distributed locking (e.g., ShedLock) would be needed if scaled out.
