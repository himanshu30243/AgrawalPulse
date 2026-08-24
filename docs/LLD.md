# AgrawalPulse — Detailed Low-Level Design (LLD)

Companion to `HLD.md`. Covers module internals, package structure, and key sequences at implementation granularity. Entity fields live in `database-schema.md`; endpoint contracts live in `api-specifications.md`.

## 1. Backend Package Structure (Maven multi-module reactor, 6 microservices)

**Superseded from a modular monolith to real microservices** — see `docs/microservices-contract.md` for the full rationale and inter-service contract. Current layout:

```
backend/
├── pom.xml                    parent reactor POM (packaging=pom, lists all 7 modules below)
├── common/                    shared library (NOT a deployable service)
│   └── src/main/java/com/agrawalpulse/common/
│       ├── tenant/            TenantContext, CurrentTenantResolver (JWT → chapter_id/roles)
│       ├── security/          SecurityConfig, JwtRolesConverter, local/ (dev JWT issuer, @Profile("local"))
│       ├── exception/         GlobalExceptionHandler, ApiError, domain exceptions
│       ├── entity/            BaseEntity
│       ├── notification/      NotificationPublisher port + Local/Sns impls
│       ├── config/             OpenApiConfig
│       └── model/              Gender, MaritalStatus (shared enums — see note below)
├── user-service/               (port 8081) owns: chapters, app_users
├── family-service/             (port 8082) owns: families, family_members
├── membership-service/         (port 8083) owns: memberships, membership_payments
├── matrimony-service/          (port 8084) owns: matrimony_consents
├── event-service/              (port 8085) owns: events, event_registrations
└── analytics-service/          (port 8086) owns: nothing — reads other services' tables directly (documented exception)
```

Each service module has its own internal `controller/`, `service/`, `repository/`, `entity/`, `dto/` sub-packages, plus a `client/` sub-package for any service that calls another service over REST (`membership-service`, `event-service`, `matrimony-service` all have a `FamilyClient`/`MatrimonyClient` calling `family-service`). Every service's `@SpringBootApplication` sets `scanBasePackages` to include both its own package and `com.agrawalpulse.common`, since `common` is a separate JAR dependency, not a subpackage.

**Why `Gender`/`MaritalStatus` live in `common`**: both `family-service` (which owns the data) and `matrimony-service` (which needs the same value semantics when computing readiness from `family-service`'s REST response) need identical enum values. JVM classes can't be shared across independently-deployed services any other way without duplicating the enum definition, so `common` — a compile-time library dependency, not a running service — is the one place code-sharing without a network hop still makes sense.

**Module boundary rule** (carried over from the monolith design, now enforced by process boundaries instead of just package-privacy): a service's entities and repositories are never touched from outside that service's JAR — full stop, since there's no in-process way to do so anymore. Cross-service reads go through the fixed REST contract in `microservices-contract.md`, with the calling service forwarding the original caller's JWT rather than using a separate service-identity system.

## 2. Tenant Context
`TenantContext` is populated once per request by `JwtAuthFilter` from the validated JWT's `chapter_id` and `roles` claims, then read by every repository via a Spring Data `@Where` clause / base repository method — application code never manually threads `chapter_id` through method signatures except at the module's public service boundary. National-role requests explicitly bypass the default chapter filter via a distinct repository method (`findAllChaptersScoped`), never implicitly.

## 3. Key Sequences

### 3.1 Family Registration
1. `POST /api/v1/families` (Chapter Admin JWT) → `FamilyController`
2. `FamilyService.register(dto)` — validates, persists `Family` + `FamilyMember` rows in one transaction
3. Publishes `FamilyRegistered` domain event (in-process event, local `ApplicationEventPublisher`)
4. `integration.notification` listener → no-op locally, SES welcome email in cloud profiles
5. `analytics` listener → increments chapter family-count read model

### 3.2 Membership Renewal
1. `POST /api/v1/memberships/{familyId}/renew` (hits `membership-service`, port 8083) with payment details
2. `MembershipServiceImpl` calls `FamilyClient.familyExists(familyId)` → `GET family-service:8082/api/v1/families/{id}`, forwarding the caller's Bearer JWT unchanged. A 404 becomes a 400 to the original caller ("family not found") — the cross-service equivalent of the old in-process check.
3. `MembershipService.renew()` — idempotent on `(familyId, year)`; if a row already exists for that year, returns existing record rather than duplicating
4. Persists `MembershipPayment`, updates `Membership.status = ACTIVE`
5. Publishes `MembershipRenewed` → notification (receipt) + analytics (collection dashboard) listeners

### 3.3 Matrimony Consent & Visibility
1. `FamilyMember` (owned by `family-service`) has no matrimony data visible by default.
2. `POST /api/v1/matrimony/consent` (self or guardian, hits `matrimony-service`, port 8084) creates a `MatrimonyConsent` row with scope (`CHAPTER`/`NATIONAL`) — requires only standard authentication, deliberately **not** `MATRIMONY_VIEWER` (self-service opt-in must not require the search/viewer permission).
3. `MatrimonyService.searchEligible(...)` — the *only* query path into matrimony data, and the only place `matrimony-service` ever calls out to `family-service` — calls `GET family-service:8082/api/v1/families/census-candidates?chapterId=...` (forwarding the caller's JWT), filters the response to members with a live (`revoked_at IS NULL`) `MatrimonyConsent`, **then** computes readiness (age/gender threshold) on that already-consent-filtered set — never the other order, so no unfiltered intermediate result can leak to a caller by mistake. Requires the caller to hold `ROLE_MATRIMONY_VIEWER`, never OR'd with admin roles. `matrimony-service` has no JPA mapping to `family_members`/`families` at all — this is enforced by the service boundary, not just a convention.
4. On write, the same service pushes/removes the member's document in the OpenSearch matrimony index (real in cloud profiles; in-memory stub locally).

### 3.4 Analytics Dashboard Read
1. `GET /api/v1/analytics/summary` (hits `analytics-service`, port 8086) — Chapter Admin sees chapter-scoped aggregate; National Admin sees cross-chapter aggregate.
2. Currently backed by live raw SQL (`NamedParameterJdbcTemplate`) against `family-service`'s and `membership-service`'s tables directly, cached in Redis for 5 minutes — this is `analytics-service`'s documented exception to "never touch another service's tables" (see `microservices-contract.md`). The originally-specified `chapter_analytics_mv` materialized view is not yet wired in; worth closing before approaching the 3-year-horizon scale target, since live cross-table aggregation is exactly what the materialized view was meant to avoid.

## 4. Frontend Structure (Vite + React + TS + MUI)

```
frontend/src/
├── app/            routes, AuthProvider, theme (responsive breakpoints)
├── api/             axios instance (Bearer token, VITE_API_BASE_URL)
├── locales/         en/, hi/ translation.json
├── modules/
│   ├── family/       registration form, list
│   ├── membership/    status view, collection dashboard
│   ├── matrimony/     consent toggle gate, eligible-member views
│   ├── event/         list, registration
│   └── analytics/     charts (recharts)
└── components/       shared layout (responsive AppBar/Drawer), role-aware nav
```

Role-aware nav mirrors backend authority checks: a nav item for Matrimony only renders if the decoded JWT carries `ROLE_MATRIMONY_VIEWER` — client-side hiding is UX only, the backend `@PreAuthorize` check is the actual control.

## 5. Idempotency & Consistency Notes
- Membership renewal and family registration are idempotent by natural key, not just "retry-safe by accident" — required because EventBridge/SNS/SES delivery is at-least-once in the cloud profiles.
- Analytics read models are eventually consistent (seconds-scale lag from write to dashboard reflection) — acceptable per requirements; the dashboards are not the system of record.
