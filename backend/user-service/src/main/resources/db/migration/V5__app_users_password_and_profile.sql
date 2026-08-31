-- Adds real password-based authentication and self-registration profile data to app_users.
--
-- email is renamed to email_address (only the physical column - the Java entity field, DTOs, and
-- JWT "email" claim all stay named "email" via an explicit @Column mapping, so this migration
-- doesn't cascade into a rename of every reference to it across six services).
--
-- Name/DOB/gender/mobile_number/password_hash are all nullable at the DB level even though the
-- registration API requires them: rows created before this migration (or via the admin
-- CreateUserRequest flow, which still only asks for email/role) have none of this and must remain
-- valid - same "legacy rows have NULL X" precedent as family-service's V2__family_owner.sql. An
-- account with a NULL password_hash simply can never pass DbLocalCredentialAuthenticator's
-- password check, which is the correct behavior for a row that predates password login entirely.

ALTER TABLE app_users RENAME COLUMN email TO email_address;
ALTER TABLE app_users RENAME CONSTRAINT uq_app_users_email TO uq_app_users_email_address;

ALTER TABLE app_users
    ADD COLUMN first_name     VARCHAR(75),
    ADD COLUMN middle_name    VARCHAR(75),
    ADD COLUMN last_name      VARCHAR(75),
    ADD COLUMN date_of_birth  DATE,
    ADD COLUMN gender         VARCHAR(20),
    ADD COLUMN mobile_number  VARCHAR(20),
    ADD COLUMN password_hash  VARCHAR(255),
    ADD COLUMN status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN updated_at     TIMESTAMPTZ NOT NULL DEFAULT now();

-- Partial (WHERE mobile_number IS NOT NULL) so multiple NULLs (rows predating this column) don't
-- conflict with each other - same pattern as families.mobile_number's unique index.
CREATE UNIQUE INDEX uq_app_users_mobile_number ON app_users (mobile_number) WHERE mobile_number IS NOT NULL;
