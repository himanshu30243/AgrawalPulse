-- Splits membership-read visibility into explicit chapter/state/all tiers, mirroring
-- V3__family_visibility_scopes.sql exactly. Until now membership-service had no scope concept at
-- all: every VIEW_MEMBERSHIP/MANAGE_MEMBERSHIP holder saw only their own chapter regardless of
-- role (membership-service's old code hardcoded chapter_id filtering with no permission check),
-- so CHAPTER_ADMIN/STATE_ADMIN/NATIONAL_ADMIN were all silently limited to one chapter - there was
-- no way for a STATE_ADMIN/NATIONAL_ADMIN to see beyond it, and no permission encoding "own family
-- only" vs "whole chapter" the way VIEW_FAMILY vs VIEW_CHAPTER_FAMILIES already does for families.
--
-- VIEW_MEMBERSHIP remains the base permission (own-family status/transactions only - see
-- MembershipAccessScope, whose own-tier delegates to family-service rather than a local
-- owner_user_id column, since Membership has none by design). MANAGE_MEMBERSHIP is unchanged.

INSERT INTO permissions (permission_code, permission_name, description) VALUES
    ('VIEW_CHAPTER_MEMBERSHIP', 'View Chapter Membership', 'Read every membership record within the caller''s own chapter.'),
    ('VIEW_STATE_MEMBERSHIP',   'View State Membership',   'Read every membership record within the caller''s own state (all chapters in it).'),
    ('VIEW_ALL_MEMBERSHIP',     'View All Membership',     'Read every membership record nationwide.');

-- CHAPTER_ADMIN: add the chapter-scope permission. It already held VIEW_MEMBERSHIP/MANAGE_MEMBERSHIP
-- with the old hardcoded chapter-only behavior - this makes that scope explicit rather than implicit.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_CHAPTER_MEMBERSHIP'
WHERE r.role_code = 'CHAPTER_ADMIN';

-- STATE_ADMIN: give it its real intended scope (every chapter in its own state) - previously it was
-- silently narrowed to just its own chapter by membership-service's old code, same class of gap V3
-- fixed for families.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_STATE_MEMBERSHIP'
WHERE r.role_code = 'STATE_ADMIN';

-- NATIONAL_ADMIN: nationwide, same reasoning.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_ALL_MEMBERSHIP'
WHERE r.role_code = 'NATIONAL_ADMIN';

-- ADMIN: explicit snapshot grant of all three, matching V3's approach for families - V2's original
-- ADMIN grant was a CROSS JOIN against permissions as they existed when V2 ran, which does not
-- retroactively pick up permissions added by later migrations (Flyway migrations are immutable
-- once applied), so every migration since V3 has had to explicit-grant its own new permissions to
-- ADMIN, same as this one does.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN ('VIEW_CHAPTER_MEMBERSHIP', 'VIEW_STATE_MEMBERSHIP', 'VIEW_ALL_MEMBERSHIP')
WHERE r.role_code = 'ADMIN';

-- USER is untouched - it keeps VIEW_MEMBERSHIP (own-family only) and never gets a tier permission,
-- matching family's "own-record is the no-permission fallback" convention exactly.
