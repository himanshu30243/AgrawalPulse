-- FY reinterpretation: memberships.year now means "financial year start year" (Apr-Mar, India),
-- not a calendar year. No column change needed - see FinancialYearUtil for the single place this
-- is computed. Existing rows' year values were already calendar years entered 1:1 with intent, so
-- no data rewrite is needed for this column.
COMMENT ON COLUMN memberships.year IS
    'Financial year start year (India, Apr-Mar). E.g. 2026 = FY 2026-27. See FinancialYearUtil.';

-- Status model expands from {ACTIVE, INACTIVE} to {ACTIVE, PENDING_RENEWAL, EXPIRED}. EXPIRED is
-- a family-level roll-up computed by MembershipServiceImpl across a family's FY rows (2+ FYs with
-- no payment, or no FY row ever paid) - it is never written to this column, only ACTIVE (this FY's
-- fee paid) or PENDING_RENEWAL (FY row exists, not yet paid) are. Legacy INACTIVE rows are
-- reinterpreted as PENDING_RENEWAL, the closest equivalent ("not yet paid" - this feature has no
-- production data yet, only 2 mock families from prior sessions, so a direct rewrite is safe and
-- much simpler than teaching the Java enum to keep a deprecated value alive forever).
UPDATE memberships SET status = 'PENDING_RENEWAL' WHERE status = 'INACTIVE';
ALTER TABLE memberships ALTER COLUMN status SET DEFAULT 'PENDING_RENEWAL';

-- membership_payments: rename paid_at -> created_at (system record-creation instant, unchanged
-- semantics/values, just a clearer name now that a *separate* admin-entered payment_date exists).
-- This is the schema half of the fix for recordPayment always stamping Instant.now() regardless of
-- what the payment was actually for - there was previously no field to hold an admin-entered date.
ALTER TABLE membership_payments RENAME COLUMN paid_at TO created_at;

ALTER TABLE membership_payments ADD COLUMN payment_date DATE;
UPDATE membership_payments SET payment_date = created_at::date;
ALTER TABLE membership_payments ALTER COLUMN payment_date SET NOT NULL;

ALTER TABLE membership_payments ADD COLUMN remarks VARCHAR(500);

-- Nullable: legacy rows (and any created by a background process with no authenticated caller)
-- have no known actor, same convention as families.owner_user_id being nullable for pre-existing
-- rows (see family-service's V2__family_owner.sql comment).
ALTER TABLE membership_payments ADD COLUMN created_by UUID;
ALTER TABLE membership_payments ADD COLUMN updated_by UUID;
ALTER TABLE membership_payments ADD COLUMN updated_at TIMESTAMPTZ;

CREATE INDEX idx_membership_payments_payment_date ON membership_payments (payment_date);
