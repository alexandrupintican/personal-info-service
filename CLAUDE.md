# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

The first real endpoint (`GET /api/v1/technologies`) is implemented, establishing the layered pattern (entity →
repository → service → controller → DTO, Flyway migrations, centralized `@RestControllerAdvice` error handling) to
follow for future endpoints — see `docs/api/contracts.md` and `docs/decisions/architecture-decisions.md`. When
implementing further features, follow this established pattern rather than inventing a new one; check with the user
if a needed convention isn't already covered by a prior ADR.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows / `./mvnw` in bash), not a global `mvn`.

- Build: `mvnw.cmd clean install`
- Run the app: `mvnw.cmd spring-boot:run`
- Run all tests: `mvnw.cmd test`
- Run a single test class: `mvnw.cmd test -Dtest=PersonalInfoServiceApplicationTests`
- Run a single test method: `mvnw.cmd test -Dtest=PersonalInfoServiceApplicationTests#contextLoads`

## Stack and architecture

- **Java 25**, **Spring Boot 4.1.1** (parent POM), Maven build.
- **Spring Data JPA** + **PostgreSQL** driver — persistence is expected but no entities/repositories exist yet.
- **Spring Security OAuth2 Client** (`spring-boot-starter-security-oauth2-client`) — the service acts as an OAuth2
  client (e.g. login via an external provider), not an OAuth2 resource server/authorization server. Corresponding
  `*-test` starters (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-oauth2-client-test`) are on
  the test classpath for JPA slice tests and OAuth2 client test support.
- **Lombok** is available (annotation processor wired into both compile and test-compile in `pom.xml`).
- **spring-boot-docker-compose** (runtime, optional) auto-starts `compose.yaml` when running the app locally via
  Spring Boot tooling — mechanically still true, but **`compose.yaml`'s Postgres (`mydatabase`, port 5433) is
  currently unused by any feature.** The real `technologies` data lives in a separate, pre-existing, externally
  -managed native Postgres install (`localhost:5432`, db `personaldb`) that predates this service — see ADR-0005 in
  `docs/decisions/architecture-decisions.md` for why, and revisit this discrepancy before adding more tables rather
  than assuming `compose.yaml` reflects where data actually lives. (`spring.docker.compose.enabled` is also
  currently `false` in `application.yaml`, so auto-start doesn't fire in practice either way.)
- **spring-boot-devtools** (runtime, optional) is on the classpath for local dev live-restart.
- `application.yaml` sets `spring.datasource.*` pointing at `personaldb` (see above). The password is
  `${DATABASE_PASSWORD}` with no default — set that environment variable before `mvnw.cmd spring-boot:run` or any
  test/tool that hits this datasource directly (the Testcontainers-backed repository test is unaffected and needs
  no such setup). Never commit the real password.
- **Flyway** (`spring-boot-starter-flyway` + `flyway-core` + `flyway-database-postgresql`) manages schema migrations
  under `src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto` is `validate`, not `update`.
  `spring.flyway.baseline-on-migrate`/`baseline-version` are set because `personaldb`'s schema predates Flyway —
  see ADR-0005.
