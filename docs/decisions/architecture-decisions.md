# Architecture Decision Log

Record every significant architectural decision here — service splits,
gateway technology choice, auth mechanism choices, schema tradeoffs,
anything that would confuse a future reader (human or agent) if left
unexplained. Both `vue3-typescript-specialist` (repo `personal-project-vue3`)
and `java-springboot-specialist` (repo `personal-info-service`) may add
entries. **This file is mirrored in both repos** — update both copies when
adding an entry, since neither agent can see the other repo by default.

Format:

```
## ADR-000N: <short title>
Date: YYYY-MM-DD
Status: proposed | accepted | superseded by ADR-000M

**Context:** what problem/requirement prompted this decision.
**Decision:** what was decided.
**Consequences:** tradeoffs, what this rules out, what it enables.
```

---

## ADR-0001: Start as a modular monolith, not microservices
Date: 2026-09-16
Status: accepted

**Context:** The multi-service reference architecture in this project's
agent briefs shows a Vue frontend behind an API gateway fanning out to
several independent Spring Boot services, each with its own database. At
present this application has one backend service (`personal-info-service`)
with no genuinely independent scaling, deployment, or ownership needs.

**Decision:** New backend business logic starts inside `personal-info-service`
as a single Spring Boot service (a modular monolith), organized into
packages by business capability, backed by one PostgreSQL database.
Additional services beyond the gateway (see ADR-0002) are introduced only
when a concrete requirement demands it (see criteria in
[`../architecture/README.md`](../architecture/README.md)), with the
justification recorded here at that time.

**Consequences:** Faster iteration and simpler operations now; a future
service split is expected to extract a module along its existing package
boundary rather than requiring a redesign, as long as module boundaries are
kept clean (no cross-module direct repository access).

---

## ADR-0002: Introduce a dedicated API Gateway in front of backend services
Date: 2026-09-16
Status: accepted (existence only) — technology/routing scope not yet decided

**Context:** The frontend (`personal-project-vue3`) needs a stable place to
send API requests as the backend grows beyond a single service. The user
confirmed (2026-09-16) that a dedicated API Gateway component is intended,
rather than the frontend calling `personal-info-service` directly or relying
on a dev-only proxy long-term.

**Decision:** A dedicated API Gateway service will sit between the Vue
frontend and backend services, starting with `personal-info-service`. Its
existence is settled. Its technology choice (e.g. Spring Cloud Gateway vs.
something else), routing rules, and whether it terminates
authentication/authorization are **not yet decided** — do not implement or
assume specifics for the gateway itself until that work is explicitly
scoped with the user. Until the gateway exists, the frontend's temporary
Hono backend (`personal-project-vue3/server.ts`) continues to stand in for
it.

**Consequences:** New backend business logic should be built in
`personal-info-service` (not the Hono placeholder) so it isn't lost when the
placeholder is retired. When the gateway is scoped, add a new ADR here
recording the technology choice and update
[`../architecture/README.md`](../architecture/README.md)'s diagram/table
accordingly.

---

## ADR-0003: Use Flyway for database migrations
Date: 2026-09-25
Status: accepted

**Context:** `personal-info-service` needed its first real table
(`technologies`, backing `GET /api/v1/technologies`) and, per `CLAUDE.md`,
had neither Flyway nor Liquibase wired up yet — `ddl-auto: update` was only
ever meant for local scaffolding.

**Decision:** Use Flyway (`spring-boot-starter-flyway` +
`flyway-core` + `flyway-database-postgresql`) for schema migrations, not
Liquibase — simpler for a solo project with one database. Migrations live
under `src/main/resources/db/migration` (`V1__create_technologies_table.sql`
is the first). `spring.jpa.hibernate.ddl-auto` is now `validate`, not
`update` — Hibernate checks the schema Flyway produced instead of mutating
it.

**Consequences:** Every future schema change ships as a new `V<n>__*.sql`
file, never a hand-edited running schema. Note for Spring Boot 4.1.1
specifically: the Flyway *engine* (`flyway-core`) and Spring Boot's Flyway
*autoconfiguration glue* (`spring-boot-flyway`, which wires
`FlywayAutoConfiguration` so migrations actually run before JPA validates
the schema) are separate artifacts in this Boot version — both are required;
`spring-boot-starter-flyway` bundles the glue.

---

## ADR-0004: Interim `permitAll` SecurityConfig until auth is decided
Date: 2026-09-25
Status: accepted (interim) — superseded once the real auth mechanism is chosen

**Context:** Implementing `GET /api/v1/technologies` (the first endpoint)
surfaced that `spring-boot-starter-security-oauth2-client` being on the
classpath makes Spring Security auto-configure a default security filter
chain that requires authentication (HTTP Basic with a generated password)
for every request — confirmed by curling the running app and getting a 401.
This contradicts `docs/api/contracts.md`, which documents this project's
auth mechanism as **not yet decided** and this endpoint as requiring no
auth.

**Decision:** Add `com.example.personalinfoservice.common.security.SecurityConfig`,
a single `SecurityFilterChain` bean that `permitAll()`s every request and
disables CSRF (no session-based state changes exist yet). It does not
implement, assume, or wire up any part of an actual OAuth2 login flow — it
only stops the default lockdown from contradicting the documented "no auth
decided yet" state.

**Consequences:** Every endpoint is unauthenticated until this is replaced.
When the real auth mechanism is decided (see the "NOT YET DECIDED" section
of `docs/api/contracts.md`), replace this config's `permitAll()` with real
`authorizeHttpRequests` rules per endpoint rather than layering rules on top
of it.

---

## ADR-0005: `technologies` data lives in a pre-existing, externally-managed Postgres — not this repo's compose-managed one
Date: 2026-09-25
Status: accepted

**Context:** The initial implementation of `GET /api/v1/technologies`
(ADR-0003, ADR-0004) assumed the frontend's Hono placeholder read from the
same Postgres this repo's `compose.yaml` provisions (`localhost:5433`,
db `mydatabase`) — that instance was empty, so a fresh `technologies` table
was migrated there and treated as greenfield. This was wrong. The user
confirmed the Hono placeholder's `.env` actually defaults `DATABASE_PORT` to
**5432**, a second, native (non-Docker) Postgres install already running on
the dev machine (`localhost:5432`, db `personaldb`, user `postgres`,
Postgres 18.4) — not the `postgres:latest` container `compose.yaml` starts.
That instance already held the real 16-row `technologies` table, created
outside this project, with a schema this service's first migration got
wrong in several ways:
- `category` is a genuine Postgres native enum type (`technology_category`,
  values `stack` / `in_progress`), not a varchar.
- No `created_at` column exists.
- `id` is `integer` (via `technologies_id_seq`), not `bigint`.
- There's a `(name, category)` unique constraint.
- No `flyway_schema_history` table — Flyway has never touched this database.

**Decision:** Point `personal-info-service` at the real, pre-existing
database (`jdbc:postgresql://localhost:5432/personaldb`) instead of copying
its data into the compose-managed instance. The `V1__create_technologies_table.sql`
migration was rewritten to be a faithful replica of the real schema (native
enum type, no `created_at`, correct id type, the unique constraint). Because
`personaldb` already has this schema with no Flyway history,
`spring.flyway.baseline-on-migrate: true` + `baseline-version: 1` is set so
Flyway baselines the existing schema at v1 instead of trying to re-run
`CREATE TABLE` against it (which would fail on "relation already exists").
This baselining only triggers against a non-empty schema with no history
table — a genuinely empty database (e.g. the Testcontainers container in
`TechnologyRepositoryTest`) still runs V1 for real, so it stays an accurate,
independently-verified replica of production rather than a migration that's
never actually exercised.

The database password is supplied via the `DATABASE_PASSWORD` environment
variable (`spring.datasource.password: ${DATABASE_PASSWORD}` in
`application.yaml`, no default) — never committed in plaintext. The
username (`postgres`) and connection topology (host/port/db name) are not
treated as secrets and are committed as-is.

**Consequences:**
- `compose.yaml`'s `postgres:latest` container (port 5433) currently has no
  consumer — nothing in this service reads or writes to it anymore. This
  contradicts `CLAUDE.md`'s documented story that `spring-boot-docker-compose`
  auto-starts the datastore this service needs; that auto-start still works
  mechanically, it just no longer provisions data this feature (or, as of
  this writing, any feature) actually uses. `spring.docker.compose.enabled`
  was already `false` locally before this ADR, so no auto-start currently
  fires in practice either way. This should be revisited (either point real
  future tables at the compose-managed instance and migrate `personaldb`'s
  data there, or accept `personaldb` as this service's real datastore going
  forward and reduce/retire `compose.yaml`) rather than left silently stale.
- Anyone running this service locally (`mvnw.cmd spring-boot:run` or any
  test/tool that hits the real datasource directly — the Testcontainers
  repository test is unaffected) must have `DATABASE_PASSWORD` set in their
  environment first, and must have a Postgres reachable at
  `localhost:5432/personaldb` with the real schema. Neither is currently
  documented as a machine setup prerequisite anywhere else in this repo —
  worth adding to a setup/README doc if one is created.
- The frontend's `Technology` TS model already types `category: string`, so
  no frontend-visible contract change is needed — the enum serializes as its
  raw string value (`"stack"` / `"in_progress"`) exactly like the Hono
  placeholder returned it.
