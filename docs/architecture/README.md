# Architecture Overview

This document is the shared reference for the frontend (`vue3-typescript-specialist`,
repo `personal-project-vue3`) and backend (`java-springboot-specialist`, repo
`personal-info-service`) agents. Because these are two separate git
repositories, **this file is mirrored** — an identical copy lives at
`personal-project-vue3/docs/architecture/README.md`. Whichever agent changes
it should update both copies (or flag the change explicitly if it only has
access to one repo). Add significant decisions to
[`../decisions/architecture-decisions.md`](../decisions/architecture-decisions.md)
(also mirrored) rather than only editing this file silently.

## Target shape (decided, not yet fully built)

```
                ┌─────────────────────┐
                │      Vue 3 App      │
                │ TypeScript / Vite   │
                └──────────┬──────────┘
                           │ HTTPS / REST
                           ▼
                ┌─────────────────────┐
                │     API Gateway     │   ← planned, not yet built
                └──────────┬──────────┘      (technology/routing TBD)
                           ▼
                ┌─────────────────────┐
                │ personal-info-service│
                │   (Spring Boot)      │
                └──────────┬───────────┘
                           ▼
                      PostgreSQL
```

The user has decided a dedicated API Gateway will exist (see ADR-0002) —
its existence is settled, but its technology choice and routing rules are
not, and should not be invented as a side effect of an unrelated feature.

## Current interim state (as of 2026-09-16)

- **Frontend repo (`personal-project-vue3`)** ships a small temporary
  Hono/Node backend (`server.ts`) as a **placeholder standing in for the
  API Gateway**, expected to be retired once the gateway exists. It no
  longer queries Postgres directly — its `GET /api/technologies` DB-backed
  route was removed now that `GET /api/v1/technologies` is implemented by
  `personal-info-service`. The frontend calls that service directly for
  local dev via a `vite.config.js` proxy (`/api/v1/*` → `http://localhost:8080`,
  where `personal-info-service` runs via `mvnw.cmd spring-boot:run` — there is
  no `Dockerfile` for the app itself yet; `compose.yaml` only provisions a
  Postgres container that this feature currently doesn't even use, see
  ADR-0005).
- **Backend repo (`personal-info-service`)** is a Spring Boot 4.1.1 / Java 25
  application. It has its first real endpoint (`GET /api/v1/technologies`,
  see `../api/contracts.md`), established as the reusable layered pattern
  (entity → repository → service → controller → DTO, Flyway migrations,
  centralized `@RestControllerAdvice` error handling) for endpoints that
  follow. It uses `spring-boot-starter-security-oauth2-client` (OAuth2
  **Client**, not Resource Server). Because no auth mechanism is decided
  yet, a temporary `SecurityConfig`
  (`com.example.personalinfoservice.common.security.SecurityConfig`) permits
  all requests — otherwise Spring Security's default lockdown (triggered by
  the OAuth2 client starter being on the classpath) would 401 every
  endpoint. Replace it with real rules once auth is decided.
- **Database reality check (see ADR-0005):** the `technologies` data does
  **not** live in this repo's `compose.yaml`-provisioned Postgres
  (`localhost:5433`, db `mydatabase`) — that instance is currently unused by
  any feature. It lives in a pre-existing, externally-managed, native
  Postgres install on the dev machine (`localhost:5432`, db `personaldb`),
  which the frontend's Hono placeholder was already reading from. This
  service's datasource points there. `spring-boot-docker-compose`'s
  auto-start still works mechanically but no longer provisions the actual
  datastore this feature needs — don't assume `compose.yaml` reflects where
  data lives without checking the current datasource config and ADR-0005.
- **New backend business logic goes into `personal-info-service`**, not into
  the Hono placeholder — otherwise it will need to be re-implemented when
  Hono is retired.

## Guiding principle: modular monolith by default (services beyond the gateway)

Start with a **single Spring Boot service** (`personal-info-service`)
organized into clearly separated packages/modules per business capability,
backed by one PostgreSQL database. Only split out an *additional* service
(with its own database) behind the gateway when there's a concrete reason:

- genuinely independent scaling requirements,
- an independent deployment cadence,
- a real ownership boundary (separate team/on-call),
- or a hard technical constraint (e.g. a different runtime/language need).

Do not create services "for architecture's sake." Every service split beyond
the already-decided gateway must be justified in an ADR.

## Service responsibilities and database ownership

| Service | Owns (business logic) | Owns (database) | Notes |
|---|---|---|---|
| API Gateway | Routing, cross-cutting concerns (TBD: auth termination, rate limiting) | none | Planned, not yet built. Technology TBD. |
| personal-info-service | Technologies/skills reference data (`GET /api/v1/technologies`, see `../api/contracts.md`) — the first endpoint, absorbed from the temporary Hono `GET /api/technologies` placeholder. More business capabilities land here as they're built. | Postgres `personaldb` (pre-existing, externally-managed native install, `localhost:5432` — **not** this repo's `compose.yaml` instance on port 5433, which is currently unused; see ADR-0005), schema managed by Flyway migrations under `src/main/resources/db/migration`. | First/primary backend service. |

Rule: **a service's database is never accessed directly by another service.**
Cross-service reads/writes happen over REST (or an event, when eventual
consistency is acceptable) — never a shared JDBC connection or shared schema.

```
GOOD:  Order Service ──REST/event──▶ User Service ──▶ User DB
BAD:   Order Service ───────────────▶ User Service's PostgreSQL database
```

## Inter-service communication rules

- Synchronous need ("I need this data now to respond") → REST call to the
  owning service.
- Asynchronous/eventual-consistency need ("I need to react when this
  happens") → an event (Kafka/RabbitMQ), only once that infrastructure is
  actually justified — don't introduce a message broker speculatively.
- No service reads another service's tables, directly or via a shared
  connection pool.

## Before building a cross-service (or cross-repo) feature

Both agents should be able to answer, before writing code:

1. Which service owns the business logic for this feature?
2. Which service owns the data involved?
3. What API/event contract is required between services (if any)?
4. What does the frontend need from the API (shape, pagination, errors)?
5. What does each backend service need from any other service it calls?
6. Does this feature depend on the API Gateway existing? If so, flag that —
   it may need to be scoped before the feature can be finished end-to-end.

See [`../api/contracts.md`](../api/contracts.md) for the API contract itself.
