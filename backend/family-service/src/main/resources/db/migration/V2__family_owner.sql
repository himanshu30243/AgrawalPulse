-- Records which user registered a family, so the "a USER may own only one family" rule can be
-- enforced server-side rather than only in the UI.
--
-- No REFERENCES app_users(id): app_users is owned by user-service, and a cross-service FK would
-- couple migration/deploy order between two independently-deployed services - the same reasoning
-- that keeps families.chapter_id unconstrained (see V1__init.sql).
--
-- Nullable on purpose. Rows created before this migration have no known owner, and back-filling
-- them by guessing (e.g. matching head-of-family email to a user) would invent ownership that was
-- never asserted. A NULL owner simply never counts toward anyone's cap.

ALTER TABLE families ADD COLUMN owner_user_id UUID;

-- Partial index: the cap check is "how many families does this user own", which never asks about
-- the unowned legacy rows.
CREATE INDEX idx_families_owner_user_id ON families (owner_user_id) WHERE owner_user_id IS NOT NULL;
