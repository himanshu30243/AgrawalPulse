-- Backs self-registration's automatic chapter resolution/creation (see UserServiceImpl#registerUser
-- and ChapterResolutionRepository): a functional unique index on lower(city)/lower(state) gives the
-- atomic "INSERT ... ON CONFLICT ... RETURNING" upsert a conflict target to detect an
-- already-existing chapter for that city+state, case-insensitively, so two sign-ups from the same
-- brand-new city can never create two separate chapters for it.
--
-- Safe to add now - the live table has exactly one row (a single "Default Chapter"), no
-- case-insensitive city+state duplicates exist to violate this constraint.
CREATE UNIQUE INDEX uq_chapters_city_state ON chapters (lower(city), lower(state));
