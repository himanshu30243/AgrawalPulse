-- user-service owns chapters + app_users only. Real FK between them is fine - both tables
-- are owned by this same service, so the FK never crosses a service-deploy boundary
-- (see docs/microservices-contract.md "Database: shared instance, service-owned tables").

CREATE TABLE chapters (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_users (
    id          UUID PRIMARY KEY,
    chapter_id  UUID NOT NULL REFERENCES chapters(id),
    email       VARCHAR(255) NOT NULL,
    cognito_sub VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_users_email UNIQUE (email),
    CONSTRAINT uq_app_users_cognito_sub UNIQUE (cognito_sub)
);
CREATE INDEX idx_app_users_chapter_id ON app_users (chapter_id);

CREATE TABLE app_user_roles (
    app_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role        VARCHAR(40) NOT NULL,
    PRIMARY KEY (app_user_id, role)
);
