-- Splits event-read visibility into explicit chapter/state/all tiers, mirroring
-- V6__membership_visibility_scopes.sql exactly (pure-additive - unlike V3__family_visibility_scopes.sql,
-- no permission here was ever over-granted in V2, so there is no DELETE step to walk back).
--
-- Today event-service has no visibility hierarchy at all: every read/write is scoped to an exact
-- chapter_id match regardless of role, so STATE_ADMIN/NATIONAL_ADMIN/ADMIN structurally cannot
-- reach events outside their own chapter even though V2 already grants them MANAGE_EVENTS. These
-- three new permissions let event-service introduce an EventAccessScope (chapter/state/all,
-- broadest wins) matching FamilyAccessScope/MembershipAccessScope's shape.

INSERT INTO permissions (permission_code, permission_name, description) VALUES
    ('VIEW_CHAPTER_EVENTS', 'View Chapter Events', 'Read every event within the caller''s own chapter.'),
    ('VIEW_STATE_EVENTS',   'View State Events',   'Read every event within the caller''s own state (all chapters in it).'),
    ('VIEW_ALL_EVENTS',     'View All Events',     'Read every event nationwide.');

-- CHAPTER_ADMIN: add the chapter-scope permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_CHAPTER_EVENTS'
WHERE r.role_code = 'CHAPTER_ADMIN';

-- STATE_ADMIN: its real intended scope.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_STATE_EVENTS'
WHERE r.role_code = 'STATE_ADMIN';

-- NATIONAL_ADMIN: nationwide.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code = 'VIEW_ALL_EVENTS'
WHERE r.role_code = 'NATIONAL_ADMIN';

-- ADMIN: explicit snapshot grant of all three.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN ('VIEW_CHAPTER_EVENTS', 'VIEW_STATE_EVENTS', 'VIEW_ALL_EVENTS')
WHERE r.role_code = 'ADMIN';
-- USER is untouched - stays at VIEW_EVENTS only (own-chapter fallback, same as today).
