# AgrawalPulse Backend

Six independently-deployable Spring Boot 3 / Java 21 microservices, built as one Maven reactor. See
[`docs/microservices-contract.md`](../docs/microservices-contract.md) for the full service split,
data-ownership rules, and inter-service contract — this file is just the how-to-run.

## Modules

| Module | Deployable? | Port | Owns |
|---|---|---|---|
| `common` | No — shared library | — | `TenantContext`, `SecurityConfig`, local dev JWT issuer, exceptions, `NotificationPublisher`, shared enums |
| `user-service` | Yes | 8081 | `chapters`, `app_users` |
| `family-service` | Yes | 8082 | `families`, `family_members` |
| `membership-service` | Yes | 8083 | `memberships`, `membership_payments` |
| `matrimony-service` | Yes | 8084 | `matrimony_consents` |
| `event-service` | Yes | 8085 | `events`, `event_registrations` |
| `analytics-service` | Yes | 8086 | nothing — reads other services' tables directly (documented exception) |

## Prerequisites

- JDK 21+ (verified against JDK 25)
- Maven 3.9+ on the PATH
- Docker Desktop (Postgres/Redis/LocalStack, and — via the root `docker-compose.yml` — the services themselves)

## Run everything locally (recommended)

From the **repo root**, not this directory:

```
docker compose up -d
```

This builds and starts all six services plus Postgres/Redis/DynamoDB-local/OpenSearch/LocalStack and a local
nginx gateway on `http://localhost:8080` that path-routes to the right service — the frontend's
`VITE_API_BASE_URL=http://localhost:8080` never has to change. See the root `README.md`.

## Run one service from source (for active development on that service)

```
mvn -pl <service-name> -am compile          # e.g. -pl family-service
mvn -pl <service-name> spring-boot:run -Dspring-boot.run.profiles=local
```

`-am` also builds `common` first. Point `SPRING_DATASOURCE_URL`/`FAMILY_SERVICE_URL`/etc. at
`localhost` (the defaults in each service's `application-local.yml`) if the rest of the stack is
running via `docker compose up -d` from the root, or leave the other five services running as
containers and only run the one you're editing from source.

## Full reactor build / verify

```
mvn clean compile   # or: mvn clean verify
```

Builds `common` then all six services in dependency order.

## Local authentication

`common`'s `security.local` package (`LocalJwtConfig`, `LocalTokenController`) is present on every
service (each scans `com.agrawalpulse.common`), so `POST /api/v1/local-auth/token` works against
any service's own port, all using the same shared HMAC secret — convenient for testing one service
in isolation. Real deployments (`dev`/`staging`/`prod`) validate Cognito-issued JWTs instead; every
class in `security.local` is `@Profile("local")`-gated and must never run outside that profile.

## Multi-tenancy & security rules baked into the code

- `chapter_id` is always read from the validated JWT (`common.tenant.CurrentTenantResolver`), never
  accepted from a request body/query parameter as the source of truth for which chapter's data to
  touch.
- No foreign key crosses a service-ownership boundary — cross-service references are plain indexed
  UUID columns, validated at write time via REST calls to the owning service (forwarding the
  caller's JWT), not by the database. See `docs/microservices-contract.md`.
- `matrimony-service` has no JPA mapping to `family_members`/`families` at all — the only path to
  member data is one REST call to `family-service`'s `census-candidates` endpoint, and the consent
  filter is always applied before readiness computation, never after.
- Matrimony *search* (`GET /api/v1/matrimony/eligible*`) requires `ROLE_MATRIMONY_VIEWER`
  specifically. Matrimony *consent* (`POST`/`DELETE /api/v1/matrimony/consent`) requires only
  standard authentication — any member manages their own consent without needing viewer rights.
