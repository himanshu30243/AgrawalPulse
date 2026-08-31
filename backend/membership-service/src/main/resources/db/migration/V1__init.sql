-- membership-service owns memberships/membership_payments only. chapter_id/family_id are
-- plain indexed UUID columns with no FK to chapters/families: those tables belong to
-- user-service/family-service respectively, and a cross-service FK would couple this service's
-- migrations/deploys to theirs (see docs/microservices-contract.md). Referential validity for
-- family_id is enforced at write time by FamilyClient's REST call to family-service instead.
-- membership_payments.membership_id keeps a real FK since memberships is owned by this same
-- service.

CREATE TABLE memberships (
    id          UUID PRIMARY KEY,
    chapter_id  UUID NOT NULL,
    family_id   UUID NOT NULL,
    year        INTEGER NOT NULL,
    fee_amount  NUMERIC(10, 2) NOT NULL DEFAULT 250,
    status      VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    paid_at     TIMESTAMPTZ,
    CONSTRAINT uq_memberships_family_year UNIQUE (family_id, year)
);
CREATE INDEX idx_memberships_chapter_id ON memberships (chapter_id);
CREATE INDEX idx_memberships_family_id ON memberships (family_id);
CREATE INDEX idx_memberships_year_status ON memberships (year, status);

CREATE TABLE membership_payments (
    id                UUID PRIMARY KEY,
    chapter_id        UUID NOT NULL,
    membership_id     UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    amount            NUMERIC(10, 2) NOT NULL,
    payment_method    VARCHAR(30) NOT NULL,
    transaction_ref   VARCHAR(150),
    paid_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_membership_payments_chapter_id ON membership_payments (chapter_id);
CREATE INDEX idx_membership_payments_membership_id ON membership_payments (membership_id);
