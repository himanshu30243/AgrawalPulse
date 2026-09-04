-- Backs the new structured family code format (SocietyCode-CityCode-000001, see
-- FamilyServiceImpl#generateFamilyCode / FamilyCodeSequenceRepository). One row per
-- (society_code, city_code) combination, holding the last sequence number issued for it -
-- a plain table rather than N Postgres SEQUENCE objects because combinations are discovered
-- dynamically as new society/city pairs appear, not known up front.
--
-- Existing families keep their old-format codes untouched - this migration only adds the
-- counter table, it does not touch the families table or backfill anything.
CREATE TABLE family_code_sequences (
    society_code   VARCHAR(3) NOT NULL,
    city_code      VARCHAR(3) NOT NULL,
    last_sequence  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (society_code, city_code)
);
