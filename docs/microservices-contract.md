# AgrawalPulse — Microservices Contract (supersedes the modular-monolith design)

This replaces the "one ECS Fargate service" framing in `HLD.md`/`LLD.md` with genuine, independently-deployable microservices — one per existing module. Kept from the earlier design: shared Aurora Postgres (not schema-per-service), tenant isolation via `chapter_id`, DPDP consent gating, the local/AWS profile-parity approach. This doc is the single source of truth for the split; every service is built against it.

## Services, ports, and table ownership

| Service | Port (local) | Owns tables | Depends on (REST) |
|---|---|---|---|
| `user-service` | 8081 | `chapter`, `app_user` | — |
| `family-service` | 8082 | `family`, `family_member` | `user-service` (chapter validation, optional) |
| `membership-service` | 8083 | `membership`, `membership_payment` | `family-service` |
| `matrimony-service` | 8084 | `matrimony_consent` | `family-service` |
| `event-service` | 8085 | `event`, `event_registration` | `family-service` |
| `analytics-service` | 8086 | none (read-only) | none — reads other services' tables directly (see exception below) |

Chapter/tenant reference data is owned by `user-service` (folds the original HLD's implicit "org structure" concern into the User module rather than inventing a 7th service).

## Database: shared instance, service-owned tables, no cross-service FKs

All services connect to the same Aurora Postgres instance/database (confirmed choice: shared DB, not schema-per-service — keeps infra cost/ops down). But because each service is deployed independently, **a Postgres FK constraint must never cross a service-ownership boundary** — it would couple migration/deploy order between two independently-deployed services, which defeats the point of splitting them.

Rule:
- **Same-service tables keep real FK constraints** (e.g. `family_member.family_id → family.id`, both owned by `family-service`).
- **Cross-service references become plain indexed UUID columns with no FK constraint** (e.g. `family.chapter_id` is a UUID column, indexed, but has no `REFERENCES chapter(id)` — `chapter` is owned by `user-service`). Referential validity across services is enforced at write time by the owning service's REST validation (see below), not by the database.

Each service runs its own Flyway migrations against its own tables only, using a **distinct migration history table name** (`spring.flyway.table=flyway_schema_history_<service>`) so six independent migration histories can coexist in one physical database without colliding.

## Inter-service communication

**Sync REST, JWT pass-through**: when a service needs to validate or read data it doesn't own, it calls the owning service's REST API directly — never a cross-service JPA repository or a cross-service SQL join. The calling service forwards the *original caller's* Bearer JWT on the outbound call, so the downstream service enforces the exact same tenant/role checks it would for a direct client call — there is no separate service-identity/service-token system to build or trust separately.

**Async events for side effects** (unchanged from the monolith design): `EventBridge` still carries domain events (`FamilyRegistered`, `MembershipRenewed`, etc.) for notification and analytics-projection purposes — this was never a synchronous path and doesn't change with the split.

### Fixed internal REST contracts (build against these exactly — no service should need to wait on another to finish)

`family-service` exposes, in addition to its public CRUD API:
- `GET /api/v1/families/{id}` — 200 with the family if it exists and belongs to the caller's chapter, 404 otherwise. This *is* the existence check other services use — no separate endpoint.
- `GET /api/v1/families/census-candidates?chapterId={id}` — internal-use list endpoint returning `CensusCandidateDto[]` (`familyMemberId, chapterId, dateOfBirth, gender, education, profession, district, maritalStatus`) for every member in the caller's chapter. `matrimony-service` calls this, then applies its own consent filter and readiness computation — it never reads `family_member` rows directly.

`membership-service`, `event-service`, `matrimony-service` each call `GET /api/v1/families/{id}` (or the candidates endpoint, for matrimony) via a typed REST client (Spring `RestClient`), forwarding the incoming `Authorization` header. A 404 from that call means "reject the write with 400 — family not found," same behavior as the old in-process check.

## Shared code: `common` module

A `backend/common/` Maven module (not a deployable service) holds only what's genuinely identical across services and would otherwise be copy-pasted six times:
- `TenantContext` / `CurrentTenantResolver` (JWT → `chapter_id`/roles)
- `SecurityConfig` base + `JwtRolesConverter` + the `security.local` dev-token package (still `@Profile("local")`-gated everywhere it's used)
- `BaseEntity`, `GlobalExceptionHandler`/`ApiError`, `ResourceNotFoundException`/`TenantAccessDeniedException`
- `NotificationPublisher` port + `LocalNoOp`/`Sns` implementations
- `OpenApiConfig`
- Shared value enums referenced across service boundaries: `Gender`, `MaritalStatus` (both `family-service` and `matrimony-service` need identical values, and JVM classes can't be shared across independently-deployed services any other way without duplicating the enum — putting them in `common` avoids that duplication)

Every service's `pom.xml` depends on `common` as a regular Maven module dependency (single reactor build: `backend/pom.xml` is a parent POM with `<packaging>pom</packaging>` and `<modules>` listing `common` + the six services).

## Local development

- One shared Postgres/Redis/OpenSearch/LocalStack (`docker-compose.yml`, already in place) — all six services point at the same Postgres instance, same as the AWS shared-database decision.
- An `nginx` container added to `docker-compose.yml` acts as a local stand-in for API Gateway, path-routing `/api/v1/users/**` → `user-service:8081`, `/api/v1/families/**` → `family-service:8082`, etc. — this means `frontend/`'s existing single `VITE_API_BASE_URL=http://localhost:8080` keeps working unchanged; the frontend never needs to know six services exist.
- Each service still runs individually via `mvn spring-boot:run -Dspring-boot.run.profiles=local -pl <service>` from `backend/`.

## AWS deployment (updates `deployment-architecture.md`)

API Gateway routes by path prefix to six ECS Fargate services (one per module) instead of one — each behind its own ALB target group, independently scaled. Terraform (not yet written) will need one ECS service + task definition per microservice rather than one.

## Analytics exception (unchanged reasoning, restated for the split)

`analytics-service` is the one deliberate exception to "never read another service's tables directly" — it's a dedicated read/reporting service (the system's CQRS read side), not a domain service, so direct cross-table SQL against the shared database is intentional, documented, and doesn't create the "distributed monolith" problem direct JPA coupling between domain services would.
