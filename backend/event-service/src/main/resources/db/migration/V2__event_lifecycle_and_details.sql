-- Adds the fields and lifecycle (Draft/Published/Cancelled) the Event Management feature needs on
-- top of V1's minimal {chapterId, title, description, eventDate, location}. See
-- docs/microservices-contract.md for event-service's ownership of this table.

ALTER TABLE events ADD COLUMN category VARCHAR(100);

-- start_time/end_time are backfilled before being made NOT NULL rather than given a column
-- DEFAULT, defensively covering any pre-existing rows even though this table is expected to be
-- empty in every real environment today (zero tests, zero seed data).
ALTER TABLE events ADD COLUMN start_time TIME;
ALTER TABLE events ADD COLUMN end_time TIME;
UPDATE events SET start_time = '09:00:00', end_time = '17:00:00' WHERE start_time IS NULL;
ALTER TABLE events ALTER COLUMN start_time SET NOT NULL;
ALTER TABLE events ALTER COLUMN end_time SET NOT NULL;

ALTER TABLE events ADD COLUMN organizer_name VARCHAR(200);
ALTER TABLE events ADD COLUMN contact_details VARCHAR(200);

ALTER TABLE events ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
CREATE INDEX idx_events_status ON events (status);

-- Nullable, same convention as families.owner_user_id/membership_payments.created_by - legacy or
-- background-actor rows have no known actor.
ALTER TABLE events ADD COLUMN created_by UUID;
ALTER TABLE events ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE events ADD COLUMN updated_by UUID;
ALTER TABLE events ADD COLUMN updated_at TIMESTAMPTZ;
