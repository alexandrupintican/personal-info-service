# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a freshly-scaffolded Spring Boot project (from Spring Initializr) with no application code yet — only the
generated skeleton (`PersonalInfoServiceApplication` main class and an empty context-load test). There are no
controllers, services, repositories, or entities. When implementing features, you are establishing the architecture,
not following an existing pattern — check with the user on package/layering conventions if it's not obvious from a
prior decision in the conversation.

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
  Spring Boot tooling (`mvnw.cmd spring-boot:run` or an IDE run configuration) — no manual `docker compose up` needed
  for local dev. `compose.yaml` defines a single `postgres:latest` service (db `mydatabase`, user `myuser`, password
  `secret`, port 5432 mapped to an ephemeral host port).
- **spring-boot-devtools** (runtime, optional) is on the classpath for local dev live-restart.
- `application.yaml` currently only sets `spring.application.name`; no datasource or security config has been added
  yet, so JPA/security config will need to be introduced when related features are implemented.
