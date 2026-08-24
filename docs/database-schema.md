# AgrawalPulse — Database Schema

> **Superseded**: the backend is now 6 microservices sharing one Aurora instance, not a monolith (see `docs/microservices-contract.md`). Each service runs its own Flyway migration, against only the tables it owns, using a distinct migration-history table name (`flyway_schema_history_<service>`) so six independent histories coexist in one physical database. **Foreign keys never cross a service-ownership boundary** — cross-service references are plain indexed UUID columns, validated at write time via REST calls (e.g. `membership-service` calling `GET family-service/api/v1/families/{id}`), not by the database. Table/column names below match the actual Flyway migrations in each service (`<service>/src/main/resources/db/migration/V1__init.sql`), which use plural table names.

## 1. Aurora PostgreSQL — by owning service

### `user-service` (port 8081)
```sql
CREATE TABLE chapters (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL REFERENCES chapters(id),  -- same-service FK: kept
    email         VARCHAR(255) NOT NULL UNIQUE,
    cognito_sub   VARCHAR(100) UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE app_user_roles (
    app_user_id   UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role          VARCHAR(30) NOT NULL,
    PRIMARY KEY (app_user_id, role)
);
CREATE INDEX idx_app_user_chapter ON app_users(chapter_id);
```

### `family-service` (port 8082)
```sql
-- Column list below reflects the family-registration wizard rewrite (see
-- frontend/docs/family-registration.md): head_of_family_name is now server-computed from
-- head_first_name/head_middle_name/head_last_name (not directly client-settable), city is
-- server-derived from district (also not client-settable), and mobile_number carries a partial
-- unique index (spec requires uniqueness; partial so multiple NULLs don't conflict).
CREATE TABLE families (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id                  UUID NOT NULL,  -- owned by user-service: no FK, indexed only
    head_first_name             VARCHAR(75) NOT NULL,
    head_middle_name            VARCHAR(75),
    head_last_name               VARCHAR(75) NOT NULL,
    head_of_family_name         VARCHAR(150) NOT NULL,  -- server-computed concatenation
    family_code                 VARCHAR(20) UNIQUE,
    family_name                 VARCHAR(150),
    head_gender                 VARCHAR(20),
    head_date_of_birth          DATE,
    mobile_number                VARCHAR(20),
    email                       VARCHAR(255),
    aadhaar_number              VARCHAR(12),
    address                     VARCHAR(500),
    district                    VARCHAR(100),
    country                     VARCHAR(100),
    state                       VARCHAR(100),
    city                        VARCHAR(100),  -- server-derived from district
    area_locality               VARCHAR(150),
    pin_code                    VARCHAR(10),
    samaj                       VARCHAR(20),   -- AGRAWAL | OTHER
    gotra                       VARCHAR(100),
    native_place                VARCHAR(150),
    occupation_business_type    VARCHAR(150),
    annual_income_range         VARCHAR(50),
    family_category             VARCHAR(30),   -- BUSINESS | SALARIED | PROFESSIONAL | RETIRED | AGRICULTURE | OTHER
    own_two_wheeler             BOOLEAN NOT NULL DEFAULT FALSE,
    own_four_wheeler            BOOLEAN NOT NULL DEFAULT FALSE,
    own_home                    BOOLEAN NOT NULL DEFAULT FALSE,
    own_plot                    BOOLEAN NOT NULL DEFAULT FALSE,
    willing_to_contribute       BOOLEAN,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_family_chapter ON families(chapter_id);
CREATE INDEX idx_family_district ON families(district);
CREATE UNIQUE INDEX idx_families_mobile_number ON families(mobile_number) WHERE mobile_number IS NOT NULL;

CREATE TABLE family_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id      UUID NOT NULL,  -- owned by user-service: no FK, indexed only
    family_id       UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,  -- same-service FK: kept
    name            VARCHAR(150) NOT NULL,
    date_of_birth   DATE NOT NULL,
    gender          VARCHAR(10) NOT NULL CHECK (gender IN ('MALE','FEMALE','OTHER')),
    education       VARCHAR(150),
    profession      VARCHAR(150),
    marital_status  VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
                     CHECK (marital_status IN ('SINGLE','MARRIED','WIDOWED','DIVORCED','UNKNOWN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_family_member_chapter ON family_members(chapter_id);
CREATE INDEX idx_family_member_family ON family_members(family_id);
-- supports marriage-readiness queries (age computed from DOB, filtered by gender + marital_status)
CREATE INDEX idx_family_member_dob_gender ON family_members(date_of_birth, gender)
    WHERE marital_status = 'SINGLE';
```
Exposes `GET /api/v1/families/{id}` (existence check other services use) and `GET /api/v1/families/census-candidates?chapterId=` (the only path `matrimony-service` has into this data — see `microservices-contract.md`).

### `membership-service` (port 8083)
```sql
CREATE TABLE memberships (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL,  -- owned by user-service: no FK
    family_id     UUID NOT NULL,  -- owned by family-service: no FK, validated via REST at write time
    year          INT NOT NULL,
    fee_amount    NUMERIC(10,2) NOT NULL DEFAULT 250.00,
    status        VARCHAR(10) NOT NULL DEFAULT 'INACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    paid_at       TIMESTAMPTZ,
    UNIQUE (family_id, year)   -- idempotency key for renewal
);
CREATE INDEX idx_membership_chapter ON memberships(chapter_id);
CREATE INDEX idx_membership_family ON memberships(family_id);
CREATE INDEX idx_membership_status ON memberships(status);

CREATE TABLE membership_payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id        UUID NOT NULL,  -- owned by user-service: no FK
    membership_id     UUID NOT NULL REFERENCES memberships(id),  -- same-service FK: kept
    amount            NUMERIC(10,2) NOT NULL,
    payment_method    VARCHAR(30) NOT NULL,
    transaction_ref   VARCHAR(100),
    paid_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_membership ON membership_payments(membership_id);
```

### `matrimony-service` (port 8084)
```sql
-- DPDP consent gate: a family_member has zero matrimony visibility without a live row here.
-- This service has NO mapping to family_members/families at all - the only path to member
-- data is one REST call to family-service's census-candidates endpoint (see LLD.md §3.3).
CREATE TABLE matrimony_consents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id        UUID NOT NULL,        -- owned by user-service: no FK
    family_member_id  UUID NOT NULL,        -- owned by family-service: no FK
    consent_given     BOOLEAN NOT NULL DEFAULT true,
    consent_scope     VARCHAR(10) NOT NULL CHECK (consent_scope IN ('CHAPTER','NATIONAL')),
    consented_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at        TIMESTAMPTZ
);
CREATE INDEX idx_consent_chapter ON matrimony_consents(chapter_id);
CREATE UNIQUE INDEX idx_consent_active_member ON matrimony_consents(family_member_id)
    WHERE revoked_at IS NULL;
```

### `event-service` (port 8085)
```sql
CREATE TABLE events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL,  -- owned by user-service: no FK
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    event_date    TIMESTAMPTZ NOT NULL,
    location      VARCHAR(200)
);
CREATE INDEX idx_event_chapter_date ON events(chapter_id, event_date);

CREATE TABLE event_registrations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL,  -- owned by user-service: no FK
    event_id      UUID NOT NULL REFERENCES events(id),  -- same-service FK: kept
    family_id     UUID NOT NULL,  -- owned by family-service: no FK, validated via REST at write time
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, family_id)
);
```

### `analytics-service` (port 8086)
No tables of its own — no Flyway migration at all. Reads `families`/`family_members`/`memberships`/`membership_payments` directly via raw SQL against the shared instance (the one documented exception to the no-cross-service-table-access rule), caching results in Redis for 5 minutes. The originally-planned `chapter_analytics_mv` materialized view (below) is not yet wired in — flagged as a scale-readiness gap in `LLD.md` §3.4.

```sql
-- Not yet implemented — analytics-service currently queries live tables directly.
-- Worth adding before approaching the 3-year-horizon scale target (~225K families).
CREATE MATERIALIZED VIEW chapter_analytics_mv AS
SELECT
    f.chapter_id,
    COUNT(DISTINCT f.id)                                          AS total_families,
    COUNT(fm.id)                                                   AS total_population,
    COUNT(DISTINCT m.family_id) FILTER (WHERE m.status='ACTIVE')  AS active_memberships,
    COALESCE(SUM(mp.amount), 0)                                    AS total_collection
FROM families f
LEFT JOIN family_members fm ON fm.family_id = f.id
LEFT JOIN memberships m ON m.family_id = f.id
LEFT JOIN membership_payments mp ON mp.membership_id = m.id
GROUP BY f.chapter_id;
```

## 2. DynamoDB (non-relational, high-write, TTL-friendly data)

Used for data that doesn't need relational integrity and benefits from DynamoDB's throughput/TTL model — **not** as a second system of record for core entities. Not currently wired into any service's code (documented target, see `AgrawalPulse-Requirements.md`).

| Table | PK | SK | Purpose | TTL | Primary consumer |
|---|---|---|---|---|---|
| `audit_log` | `chapterId` | `timestamp#eventId` | Who viewed/changed matrimony data, consent changes — DPDP accountability | none (retained) | `matrimony-service` |
| `session_cache` | `sessionId` | — | Short-lived session/refresh-token metadata | 24h | `user-service` |
| `notification_dedup` | `eventId` | — | At-least-once EventBridge delivery dedup key | 48h | any service publishing domain events |

## 3. ElastiCache Redis
- `analytics-service`: cache-aside for its aggregate queries (TTL 5 min, invalidated on relevant write events) — currently the only service using Redis (see `common/cache` note in `LLD.md`).
- Rate limiting counters for public-facing endpoints (e.g., consent opt-in link) — documented target, not yet implemented.

## 4. OpenSearch (matrimony search index only)
Owned and populated exclusively by `matrimony-service`, only for `family_member_id`s with a live `matrimony_consents` row. Index `matrimony-eligible`:
```json
{
  "familyMemberId": "uuid",
  "chapterId": "uuid",
  "district": "string",
  "ageYears": 24,
  "gender": "MALE",
  "education": "string",
  "profession": "string",
  "consentScope": "NATIONAL"
}
```
No name, DOB, address, or contact fields are indexed — search/filter only. The app resolves consented profile detail by ID after search (via `matrimony-service`'s own data, never by reaching into `family-service`'s tables), with its own authorization check.
