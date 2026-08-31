-- event-service owns events/event_registrations only (see docs/microservices-contract.md).
-- chapter_id (both tables) and family_id (event_registrations) reference rows owned by
-- user-service/family-service respectively - plain indexed UUID columns, no FK, since a
-- cross-service FK would couple this service's migration/deploy order to theirs. event_id keeps
-- a real FK because both events and event_registrations are owned by this same service.

CREATE TABLE events (
    id          UUID PRIMARY KEY,
    chapter_id  UUID NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    event_date  DATE NOT NULL,
    location    VARCHAR(300)
);
CREATE INDEX idx_events_chapter_id ON events (chapter_id);
CREATE INDEX idx_events_event_date ON events (event_date);

CREATE TABLE event_registrations (
    id              UUID PRIMARY KEY,
    chapter_id      UUID NOT NULL,
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    family_id       UUID NOT NULL,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_registrations_event_family UNIQUE (event_id, family_id)
);
CREATE INDEX idx_event_registrations_chapter_id ON event_registrations (chapter_id);
CREATE INDEX idx_event_registrations_event_id ON event_registrations (event_id);
CREATE INDEX idx_event_registrations_family_id ON event_registrations (family_id);
