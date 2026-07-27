# GitHub Push Event Ingestion Service

A Spring Boot service that polls the GitHub Public Events API, ingests Push events, and persists them with structured queryable fields in PostgreSQL.

## Architecture

The application is a single Spring Boot 4 container backed by PostgreSQL. A scheduled poller fetches events every 60 seconds, filters for `PushEvent` types, extracts structured fields, and persists events alongside actor and repository metadata. A REST API exposes querying by any combination of actor, repository, push ID, ref, head, and before SHA.

For detailed design decisions, data model, and tradeoffs see [DESIGN-BRIEF.md](DESIGN-BRIEF.md). Future enhancement designs are in [EXTENSION-DESIGN.md](EXTENSION-DESIGN.md).

## Getting Started

```bash
docker compose up --build
```

That's it. The application starts polling immediately on boot — no separate ingestion command needed.

## Running Tests

```bash
docker compose run --rm test
```

Or locally (requires Docker for Testcontainers):

```bash
mvn test
```

## How to Verify It's Working

**Logs** (`docker compose logs -f app`):

- `Polling GitHub Events API` — confirms the poller is running (appears every 60s)
- `Poll complete: ingested=N, skipped(duplicate)=M` — confirms events are being processed
- Errors are logged to stderr with context; the service does not crash on failures

**Timing:** Results appear within the first 60 seconds of startup after Liquibase migrations complete.

**Database tables:**

| Table          | Contains                                                          |
| -------------- | ----------------------------------------------------------------- |
| `push_events`  | Ingested push events with structured fields and raw JSONB payload |
| `actors`       | GitHub actor metadata (login, avatar URL, etc.)                   |
| `repositories` | GitHub repository metadata (name, URL)                            |

**API:**

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Query events: `GET http://localhost:8080/events?ref=refs/heads/main`

For full schema details and design rationale, see [DESIGN-BRIEF.md](DESIGN-BRIEF.md).
