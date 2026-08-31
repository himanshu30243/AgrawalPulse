-- matrimony-service owns matrimony_consents only. chapter_id and family_member_id are plain
-- indexed UUID columns with NO FK constraint: chapters is owned by user-service and
-- family_members is owned by family-service, both separately deployed services, and a
-- cross-service FK would couple migration/deploy order between services (see
-- docs/microservices-contract.md "Database: shared instance, service-owned tables"). Referential
-- validity is instead enforced at write time by MatrimonyServiceImpl calling family-service's
-- REST API - never by a database constraint or a local join.

CREATE TABLE matrimony_consents (
    id                 UUID PRIMARY KEY,
    chapter_id         UUID NOT NULL,
    family_member_id   UUID NOT NULL,
    consent_given      BOOLEAN NOT NULL DEFAULT false,
    consent_scope      VARCHAR(20) NOT NULL,
    consented_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at         TIMESTAMPTZ
);
CREATE INDEX idx_matrimony_consents_chapter_id ON matrimony_consents (chapter_id);
CREATE INDEX idx_matrimony_consents_family_member_id ON matrimony_consents (family_member_id);
-- A member is matrimony-visible only while a row exists here with revoked_at IS NULL -
-- this partial index makes that hard business rule cheap to check at scale.
CREATE INDEX idx_matrimony_consents_active ON matrimony_consents (family_member_id) WHERE revoked_at IS NULL;
