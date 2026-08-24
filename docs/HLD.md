# AgrawalPulse — Enterprise High-Level Design (HLD)

## 1. Purpose
AgrawalPulse is a national, multi-chapter platform for Agrawal Samaj covering family census, membership dues, marriage-readiness categorization, events, and analytics. This HLD reflects the assumptions in `AgrawalPulse-Requirements.md`: ~150 chapters / ~225,000 families at 3-year horizon, single-region (`ap-south-1`) at launch, local-first development mirrored to AWS.

> **Superseded**: §2/§3 below originally described a modular monolith. The backend is now six independently-deployable microservices, one per module — see `docs/microservices-contract.md` for the authoritative service boundaries, data-ownership rules, and inter-service contracts. This HLD is updated to match; treat the contract doc as the source of truth for anything not covered here.

## 2. System Context

```
        ┌─────────────┐        ┌─────────────┐
        │  Members /  │        │   Chapter    │
        │  Admins     │        │   Admins /   │
        │ (mobile+web)│        │   National   │
        └──────┬──────┘        │   Admins     │
               │               └──────┬───────┘
               ▼                      ▼
        ┌─────────────────────────────────────┐
        │        React SPA (responsive)         │
        └──────────────────┬────────────────────┘
                            ▼
        ┌─────────────────────────────────────┐
        │  Amazon API Gateway (REST, JWT authorizer,   │
        │  path-routes to 6 backing services)          │
        └───┬───┬───┬───┬───┬───┬──────────────────────┘
            ▼   ▼   ▼   ▼   ▼   ▼
          user family member. matri- event analytics    ← 6 ECS Fargate
          -svc -svc  -ship    mony  -svc  -svc             services
               8082  -svc     -svc  8085  8086
         8081        8083     8084
               │       │        │     │      │
               ▼       ▼        ▼     ▼      ▼
          ┌─────────────────────────────────────┐
          │     Aurora PostgreSQL (shared         │
          │  instance, service-owned tables,      │
          │  no cross-service FKs — see            │
          │  microservices-contract.md)            │
          └─────────────────────────────────────┘
          + DynamoDB (audit/session) + Redis (analytics cache) + OpenSearch (matrimony index)
```

Supporting: S3 (documents/exports), SNS+SES (notifications), EventBridge (async domain events), Athena+Glue+QuickSight (BI over exported data), Cognito (identity).

## 3. Architectural Style
- **Six independently-deployable microservices** — `user-service`, `family-service`, `membership-service`, `matrimony-service`, `event-service`, `analytics-service` — one per module, each its own ECS Fargate service/task definition, each its own deployable JAR/container. Full data-ownership rules, port assignments, and the fixed inter-service REST contract are in `docs/microservices-contract.md`.
- **Shared database, not schema-per-service**: all six connect to one Aurora Postgres instance (cost/ops tradeoff, explicitly chosen over full per-service database isolation), but each service owns specific tables, runs its own Flyway migrations (distinct history table per service), and — critically — never has a foreign key crossing a service-ownership boundary, since that would couple two independently-deployed services' migration order. Cross-service references are plain indexed UUID columns, validated at write time via REST calls instead of DB constraints.
- **Sync REST + JWT pass-through for cross-service reads**: when a service needs data it doesn't own (e.g. `membership-service` confirming a family exists), it calls the owning service's REST API directly, forwarding the original caller's Bearer JWT rather than inventing a separate service-identity system — the downstream service enforces the same tenant/role checks it would for a direct client call.
- **Event-driven for cross-service side effects only** (e.g., "membership renewed" → notification, "family registered" → analytics projection update) via EventBridge, not for core request/response flows.
- **CQRS via a dedicated read service**: `analytics-service` is the one deliberate exception to "never touch another service's tables" — it reads across `family`/`membership`/etc. tables directly via raw SQL against the shared database, since it's a read-only reporting service, not a domain service, and this avoids the "distributed monolith" problem direct JPA coupling between domain services would create.
- **Saga pattern reserved for future use**: the only genuinely multi-step distributed transaction candidate today is "membership payment → renewal status update → receipt email → analytics update," which is handled as an idempotent event chain (see §5), not a formal saga orchestrator — introducing one now, across only two services with no complex compensating-transaction need yet, would be complexity without a matching problem.
- **Distributed caching**: ElastiCache Redis in front of `analytics-service`'s aggregate queries and for rate-limiting/session data.

## 4. Multi-Tenancy
Every domain table carries `chapter_id`. Tenant scoping is enforced at three layers:
1. **JWT claim**: Cognito (or local dev issuer) embeds `chapter_id` and `roles`.
2. **API layer**: Spring Security resolves the authenticated principal's chapter before any query executes; chapter_id is never accepted from client input for write operations.
3. **Data layer**: every repository query is chapter-scoped by default; only roles with a `NATIONAL_*` authority can query across chapters, and those queries go through the Analytics module's aggregate read models, not raw per-record access.

## 5. Core Flows

### 5.1 Family registration → census
Admin submits family + members → validated, persisted (Aurora) → `FamilyRegistered` event → EventBridge → Analytics module updates chapter aggregate counts (async, eventually consistent within seconds).

### 5.2 Membership renewal
Payment recorded → `Membership.status` set ACTIVE, `paidAt` stamped → `MembershipRenewed` event → (a) SES receipt email, (b) analytics collection-dashboard update. Idempotency key = `(familyId, year)` prevents duplicate renewal records if the event is retried.

### 5.3 Marriage-readiness categorization
Computed on read (not stored) from `FamilyMember.dateOfBirth` + gender against the community thresholds (girls ≥21, boys ≥24). Only members with a **non-revoked MatrimonyConsent** row are included in matrimony dashboards or search (OpenSearch index), enforcing the DPDP consent gate at the data-access layer, not just the UI.

### 5.4 Analytics
District/education/profession/age-distribution dashboards read from materialized aggregate views refreshed on relevant domain events, not computed live per-request. National roll-ups additionally respect matrimony consent scoping — readiness *counts* can be national, individual profiles cannot.

## 6. Non-Functional Targets
- Availability: 99.5% (launch, single-region).
- RTO 4h / RPO 15min (Aurora automated backup + PITR).
- Local parity: full stack runnable via Docker Compose with zero AWS dependency (see Deployment Architecture doc).

## 7. Out of Scope for v1
- Multi-region active-active.
- Full event-sourced CQRS / saga orchestration.
- Native mobile apps (responsive web only).
