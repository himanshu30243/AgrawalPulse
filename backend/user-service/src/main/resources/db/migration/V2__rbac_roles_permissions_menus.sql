-- RBAC: role/permission/menu masters, their mappings, and a single role per user.
--
-- user-service owns all of this, matching its existing ownership of chapters + app_users
-- (docs/microservices-contract.md "Database: shared instance, service-owned tables"). Other
-- services never read these tables - they receive the caller's role and permission codes as JWT
-- claims and authorize locally, which is what keeps them independently deployable.
--
-- Postgres dialect throughout (UUID, TIMESTAMPTZ, now(), gen_random_uuid) to match V1__init.sql.

-- gen_random_uuid() is built in from PG13; the extension is a no-op safety net on older servers.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------- masters

CREATE TABLE roles (
    role_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_role_code UNIQUE (role_code)
);

CREATE TABLE permissions (
    permission_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_code VARCHAR(80)  NOT NULL,
    permission_name VARCHAR(120) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_permissions_permission_code UNIQUE (permission_code)
);

CREATE TABLE menus (
    menu_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Stable machine key the frontend uses for i18n lookup and icon mapping. menu_name is the
    -- English display fallback; menu_key is what code keys off, so renaming a menu for display
    -- never breaks the UI.
    menu_key       VARCHAR(60)  NOT NULL,
    menu_name      VARCHAR(100) NOT NULL,
    menu_path      VARCHAR(200),
    icon           VARCHAR(80),
    display_order  INT          NOT NULL DEFAULT 0,
    parent_menu_id UUID REFERENCES menus (menu_id) ON DELETE CASCADE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_menus_menu_key UNIQUE (menu_key)
);
CREATE INDEX idx_menus_parent_menu_id ON menus (parent_menu_id);

-- ---------------------------------------------------------------- mappings

CREATE TABLE role_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID NOT NULL REFERENCES roles (role_id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (permission_id) ON DELETE CASCADE,
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);
CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);

CREATE TABLE role_menus (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles (role_id) ON DELETE CASCADE,
    menu_id UUID NOT NULL REFERENCES menus (menu_id) ON DELETE CASCADE,
    CONSTRAINT uq_role_menus UNIQUE (role_id, menu_id)
);
CREATE INDEX idx_role_menus_role_id ON role_menus (role_id);

-- ---------------------------------------------------------------- seed: roles

INSERT INTO roles (role_code, role_name, description) VALUES
    ('USER',          'User',           'Self-registered community member. Owns a single family record.'),
    ('CHAPTER_ADMIN', 'Chapter Admin',  'Manages families, membership and events within one chapter.'),
    ('STATE_ADMIN',   'State Admin',    'Oversees the chapters within a state.'),
    ('NATIONAL_ADMIN','National Admin', 'Nation-wide visibility and management.'),
    ('ADMIN',         'Administrator',  'Full system access including roles, menus and permissions.');

-- ---------------------------------------------------------------- seed: permissions

INSERT INTO permissions (permission_code, permission_name, description) VALUES
    ('VIEW_DASHBOARD',            'View Dashboard',            'See the dashboard landing page.'),
    ('VIEW_FAMILY',               'View Family',               'Read family records within the caller''s scope.'),
    ('VIEW_ALL_FAMILIES',         'View All Families',         'Read families beyond the caller''s own record.'),
    ('CREATE_FAMILY',             'Create Family',             'Register a family.'),
    -- Lifts the one-family-per-user cap. Modelling the cap as a permission (rather than an
    -- "is this an admin" check) is what lets a future role register many families with no code
    -- change - see FamilyService's enforcement.
    ('CREATE_FAMILY_UNLIMITED',   'Create Unlimited Families', 'Register more than one family.'),
    ('EDIT_FAMILY',               'Edit Family',               'Modify family records in scope.'),
    ('DELETE_FAMILY',             'Delete Family',             'Remove family records.'),
    ('VIEW_MEMBERSHIP',           'View Membership',           'Read membership status and dues.'),
    ('MANAGE_MEMBERSHIP',         'Manage Membership',         'Record and renew membership payments.'),
    ('VIEW_EVENTS',               'View Events',               'Browse events.'),
    ('MANAGE_EVENTS',             'Manage Events',             'Create and administer events.'),
    -- Replaces the former MATRIMONY_VIEWER role. Kept as its own permission so matrimonial data
    -- remains a separately-grantable tier rather than something every admin inherits - the DPDP
    -- constraint documented on MatrimonyController.
    ('VIEW_MATRIMONY_DIRECTORY',  'View Matrimony Directory',  'Read consented matrimonial profiles.'),
    ('MANAGE_MATRIMONY_CONSENT',  'Manage Matrimony Consent',  'Give or withdraw matrimony consent for self/family.'),
    ('VIEW_REPORTS',              'View Reports',              'Read analytics and reports.'),
    ('MANAGE_USERS',              'Manage Users',              'Create users and assign their role/branch.'),
    ('MANAGE_ROLES',              'Manage Roles',              'Create roles and assign permissions and menus.'),
    ('MANAGE_BRANCHES',           'Manage Branches',           'Create and edit branches/chapters.');

-- ---------------------------------------------------------------- seed: menus

INSERT INTO menus (menu_key, menu_name, menu_path, icon, display_order) VALUES
    ('dashboard',        'Dashboard',         '/dashboard',          'Dashboard',           10),
    ('families',         'Family',            '/families',           'FamilyRestroom',      20),
    ('membership',       'Membership',        '/membership',         'CardMembership',      30),
    ('events',           'Events',            '/events',             'Event',               40),
    ('matrimony',        'Matrimony',         '/matrimony/consent',  'Favorite',            50),
    ('reports',          'Reports',           '/reports',            'Assessment',          60),
    ('userManagement',   'User Management',   '/admin/users',        'People',              70),
    ('branchManagement', 'Branch Management', '/admin/branches',     'AccountTree',         80),
    ('roleManagement',   'Role Management',   '/admin/roles',        'AdminPanelSettings',  90),
    ('settings',         'Settings',          '/settings',           'Settings',           100);

-- ---------------------------------------------------------------- seed: role -> permission

-- Helper pattern: join by code so no UUID is ever hardcoded in this file.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN (
    'VIEW_DASHBOARD', 'VIEW_FAMILY', 'CREATE_FAMILY', 'EDIT_FAMILY',
    'VIEW_MEMBERSHIP', 'VIEW_EVENTS', 'MANAGE_MATRIMONY_CONSENT'
)
WHERE r.role_code = 'USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN (
    'VIEW_DASHBOARD', 'VIEW_FAMILY', 'VIEW_ALL_FAMILIES', 'CREATE_FAMILY',
    'CREATE_FAMILY_UNLIMITED', 'EDIT_FAMILY',
    'VIEW_MEMBERSHIP', 'MANAGE_MEMBERSHIP', 'VIEW_EVENTS', 'MANAGE_EVENTS',
    'VIEW_MATRIMONY_DIRECTORY', 'MANAGE_MATRIMONY_CONSENT', 'VIEW_REPORTS',
    'MANAGE_BRANCHES'
)
WHERE r.role_code = 'CHAPTER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_code IN (
    'VIEW_DASHBOARD', 'VIEW_FAMILY', 'VIEW_ALL_FAMILIES', 'CREATE_FAMILY',
    'CREATE_FAMILY_UNLIMITED', 'EDIT_FAMILY',
    'VIEW_MEMBERSHIP', 'MANAGE_MEMBERSHIP', 'VIEW_EVENTS', 'MANAGE_EVENTS',
    'VIEW_MATRIMONY_DIRECTORY', 'MANAGE_MATRIMONY_CONSENT', 'VIEW_REPORTS',
    'MANAGE_BRANCHES', 'MANAGE_USERS'
)
WHERE r.role_code IN ('STATE_ADMIN', 'NATIONAL_ADMIN');

-- ADMIN gets everything, including permissions added by later migrations only if those
-- migrations repeat this pattern - deliberately an explicit snapshot, not a wildcard grant at
-- runtime, so an accidentally-added permission never silently widens admin.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'ADMIN';

-- ---------------------------------------------------------------- seed: role -> menu

INSERT INTO role_menus (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM roles r
JOIN menus m ON m.menu_key IN ('dashboard', 'families', 'membership', 'events')
WHERE r.role_code = 'USER';

INSERT INTO role_menus (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM roles r
JOIN menus m ON m.menu_key IN (
    'dashboard', 'families', 'membership', 'events', 'matrimony', 'reports', 'branchManagement'
)
WHERE r.role_code = 'CHAPTER_ADMIN';

INSERT INTO role_menus (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM roles r
JOIN menus m ON m.menu_key IN (
    'dashboard', 'families', 'membership', 'events', 'matrimony', 'reports',
    'branchManagement', 'userManagement'
)
WHERE r.role_code IN ('STATE_ADMIN', 'NATIONAL_ADMIN');

INSERT INTO role_menus (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM roles r
CROSS JOIN menus m
WHERE r.role_code = 'ADMIN';

-- ---------------------------------------------------------------- user -> single role

ALTER TABLE app_users ADD COLUMN role_id UUID REFERENCES roles (role_id);

-- Collapse the previous many-to-many app_user_roles into one role, most-privileged wins.
-- Old -> new mapping:
--   ADMIN            -> ADMIN
--   NATIONAL_ADMIN   -> NATIONAL_ADMIN
--   CHAPTER_ADMIN    -> CHAPTER_ADMIN
--   TREASURER        -> CHAPTER_ADMIN  (chapter-scoped management; its dues rights are now the
--                                       MANAGE_MEMBERSHIP permission on that role)
--   MATRIMONY_VIEWER -> USER           (directory access is now the VIEW_MATRIMONY_DIRECTORY
--                                       permission, which USER does NOT hold - previously
--                                       matrimony-only accounts lose directory access and must
--                                       be re-granted a role that carries it)
--   MEMBER           -> USER
UPDATE app_users u
SET role_id = r.role_id
FROM (
    SELECT app_user_id,
           CASE MIN(
               CASE role
                   WHEN 'ADMIN'            THEN 1
                   WHEN 'NATIONAL_ADMIN'   THEN 2
                   WHEN 'CHAPTER_ADMIN'    THEN 3
                   WHEN 'TREASURER'        THEN 4
                   ELSE 5
               END)
               WHEN 1 THEN 'ADMIN'
               WHEN 2 THEN 'NATIONAL_ADMIN'
               WHEN 3 THEN 'CHAPTER_ADMIN'
               WHEN 4 THEN 'CHAPTER_ADMIN'
               ELSE 'USER'
           END AS new_role_code
    FROM app_user_roles
    GROUP BY app_user_id
) collapsed
JOIN roles r ON r.role_code = collapsed.new_role_code
WHERE u.id = collapsed.app_user_id;

-- Any user with no role rows at all (or created before this migration) defaults to USER.
UPDATE app_users
SET role_id = (SELECT role_id FROM roles WHERE role_code = 'USER')
WHERE role_id IS NULL;

ALTER TABLE app_users ALTER COLUMN role_id SET NOT NULL;
CREATE INDEX idx_app_users_role_id ON app_users (role_id);

DROP TABLE app_user_roles;
