-- Splits family-read visibility into explicit chapter/state tiers.
--
-- Until now CHAPTER_ADMIN and STATE_ADMIN had no permission encoding their intended read scope at
-- all: family-service's listFamilies() unconditionally filtered to the caller's own chapter_id
-- regardless of role, and STATE_ADMIN was seeded (V2) with the exact same permission set as
-- NATIONAL_ADMIN - including VIEW_ALL_FAMILIES, which let a state admin read every chapter
-- nationwide, not just their own state. Two new permissions make each tier explicit and
-- independently grantable, matching this system's "permission, never role name" convention
-- (RbacController's javadoc): VIEW_CHAPTER_FAMILIES (the caller's own chapter) and
-- VIEW_STATE_FAMILIES (every chapter sharing the caller's own chapter's state).

INSERT INTO permissions (permission_code, permission_name, description) VALUES
    ('VIEW_CHAPTER_FAMILIES', 'View Chapter Families', 'Read every family record within the caller''s own chapter.'),
    ('VIEW_STATE_FAMILIES',   'View State Families',   'Read every family record within the caller''s own state (all chapters in it).');

-- CHAPTER_ADMIN: add the new chapter-scope permission. Nothing else about its grants changes -
-- it never held VIEW_ALL_FAMILIES, so its effective read scope (its own chapter) is unchanged,
-- just now backed by an explicit permission instead of family-service's old hardcoded default.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_CHAPTER_FAMILIES'
WHERE r.role_code = 'CHAPTER_ADMIN';

-- STATE_ADMIN: replace VIEW_ALL_FAMILIES (wrongly nation-wide, see above) with VIEW_STATE_FAMILIES.
-- NATIONAL_ADMIN is untouched - it keeps VIEW_ALL_FAMILIES from V2, which is correct for it.
DELETE FROM role_permissions
WHERE role_id = (SELECT role_id FROM roles WHERE role_code = 'STATE_ADMIN')
  AND permission_id = (SELECT permission_id FROM permissions WHERE permission_code = 'VIEW_ALL_FAMILIES');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_STATE_FAMILIES'
WHERE r.role_code = 'STATE_ADMIN';

-- ADMIN: same "explicit snapshot, not a wildcard" approach as V2 - grant the two new permissions
-- directly rather than relying on a runtime CROSS JOIN, so a future permission addition doesn't
-- silently widen admin without a migration author noticing.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN ('VIEW_CHAPTER_FAMILIES', 'VIEW_STATE_FAMILIES')
WHERE r.role_code = 'ADMIN';
