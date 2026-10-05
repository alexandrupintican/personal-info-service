# personal-info-service

Backend service for the "about me" personal site. Owns the `technologies`/skills
data and (over time) other personal-info resources, exposed as a versioned REST
API consumed by the `personal-project-vue3` frontend.

## Stack

- Java 25, Spring Boot 4.1.1, Maven
- Spring Web (REST controllers)
- Spring Data JPA + PostgreSQL driver
- Flyway (schema migrations)
- Spring Security OAuth2 Client (service acts as an OAuth2 client, not a
  resource/authorization server — see `docs/api/contracts.md`)
- Lombok

## Getting started

Use the Maven wrapper, not a global `mvn` install:

```shell
mvnw.cmd clean install      # build
mvnw.cmd spring-boot:run    # run
mvnw.cmd test                # test
```

### Database

The app points at a pre-existing, externally managed native Postgres instance
(`localhost:5432`, db `personaldb`) — **not** the Postgres defined in
`compose.yaml`, which is currently unused by any feature (see ADR-0005 in
`docs/decisions/architecture-decisions.md`).

Set the `DATABASE_PASSWORD` environment variable before running the app or
any test/tool that hits this datasource directly — copy `.env.example` to
`.env` and fill it in. Never commit the real password.

Schema migrations live under `src/main/resources/db/migration` and are
applied by Flyway (`spring.jpa.hibernate.ddl-auto` is `validate`, not
`update`).

## API

Endpoints are documented in [`docs/api/contracts.md`](docs/api/contracts.md)
and [`docs/api/openapi.yaml`](docs/api/openapi.yaml) — these are the shared
source of truth with the frontend and are mirrored in both repos.

Currently implemented:

- `GET /api/v1/technologies` — list of technologies/skills shown on the
  "about me" page, each with a self-rated confidence level.

## Architecture

New endpoints should follow the established layered pattern (entity →
repository → service → controller → DTO) and centralized
`@RestControllerAdvice` error handling. See
[`docs/architecture/README.md`](docs/architecture/README.md) and
[`docs/decisions/architecture-decisions.md`](docs/decisions/architecture-decisions.md)
for the full rationale and prior decisions before introducing a new
convention.
