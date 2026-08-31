-- family-service schema: families + family_members only. This service's Flyway history is
-- tracked in flyway_schema_history_family (spring.flyway.table), a separate table from every
-- other service's history so six independent migration histories can share one physical
-- Postgres database without colliding (docs/microservices-contract.md).
--
-- chapter_id is denormalized onto family_members (not just the aggregate root) and checked
-- directly by repositories - see CurrentTenantResolver / FamilyMemberRepository. This is
-- deliberate: relying on a join to families as the *only* tenant boundary is fragile at
-- ~150-chapter scale with sensitive matrimonial data - a query that omits or mis-joins loses
-- isolation silently, with no column for a WHERE clause or a DB constraint to catch it.
--
-- Neither families.chapter_id nor family_members.chapter_id has a REFERENCES chapters(id):
-- chapters is owned by user-service, a separately-deployed service, and a cross-service FK
-- would couple migration/deploy order between two independently-deployed services. Referential
-- validity is enforced at write time via REST validation instead (see contract doc). The
-- family_members.family_id -> families.id FK is kept because both tables are owned by this
-- same service.

CREATE TABLE families (
    id                          UUID PRIMARY KEY,
    chapter_id                  UUID NOT NULL,
    head_first_name             VARCHAR(75) NOT NULL,
    head_middle_name            VARCHAR(75),
    head_last_name              VARCHAR(75) NOT NULL,
    head_of_family_name         VARCHAR(150) NOT NULL,
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
    city                        VARCHAR(100),
    area_locality               VARCHAR(150),
    pin_code                    VARCHAR(10),
    samaj                       VARCHAR(20),
    gotra                       VARCHAR(100),
    native_place                VARCHAR(150),
    occupation_business_type    VARCHAR(150),
    annual_income_range         VARCHAR(50),
    family_category             VARCHAR(30),
    own_two_wheeler             BOOLEAN NOT NULL DEFAULT FALSE,
    own_four_wheeler            BOOLEAN NOT NULL DEFAULT FALSE,
    own_home                    BOOLEAN NOT NULL DEFAULT FALSE,
    own_plot                    BOOLEAN NOT NULL DEFAULT FALSE,
    willing_to_contribute       BOOLEAN,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_families_chapter_id ON families (chapter_id);
CREATE INDEX idx_families_district ON families (district);
CREATE INDEX idx_families_city ON families (city);
-- Spec requires mobile number uniqueness; partial (WHERE mobile_number IS NOT NULL) so multiple
-- NULLs (families registered before this was required, or without a mobile at all) don't
-- conflict with each other - Postgres already treats distinct NULLs as non-equal, but being
-- explicit here documents the intent rather than relying on that default behavior.
CREATE UNIQUE INDEX idx_families_mobile_number ON families (mobile_number) WHERE mobile_number IS NOT NULL;

CREATE TABLE family_members (
    id                   UUID PRIMARY KEY,
    chapter_id           UUID NOT NULL,
    family_id            UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    name                 VARCHAR(150) NOT NULL,
    relationship_to_head VARCHAR(20),
    date_of_birth        DATE NOT NULL,
    gender               VARCHAR(20) NOT NULL,
    education            VARCHAR(150),
    institute_name       VARCHAR(150),
    profession           VARCHAR(150),
    company_name         VARCHAR(150),
    mobile_number        VARCHAR(20),
    email                VARCHAR(255),
    blood_group          VARCHAR(15),
    marital_status       VARCHAR(20) NOT NULL DEFAULT 'SINGLE'
);
CREATE INDEX idx_family_members_chapter_id ON family_members (chapter_id);
CREATE INDEX idx_family_members_family_id ON family_members (family_id);
-- Supports the marriage-readiness range queries (gender + date_of_birth threshold scan) used
-- by matrimony-service via the census-candidates endpoint.
CREATE INDEX idx_family_members_gender_dob ON family_members (gender, date_of_birth);
