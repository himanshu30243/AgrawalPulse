-- Backs the family registration wizard's / edit-family dialog's PIN-code auto-fill
-- (PincodeLookupController) with our own reference data instead of a live call to India Post's
-- public API, which proved unreliably reachable from Java on this network (TLS errors and plain
-- connection resets on repeated attempts, even though curl/the OS trust the same endpoint).
--
-- Deliberately a curated subset, not an exhaustive official dataset - India has ~19,000 real PIN
-- codes, and importing the full official set isn't practical here. Same precedent already set by
-- frontend/src/data/locationData.ts's own header comment: seed the cities/states already listed
-- there, extend later via a plain INSERT-only migration as gaps are found.
CREATE TABLE pincodes (
    pincode  VARCHAR(6) PRIMARY KEY,
    district VARCHAR(100) NOT NULL,
    state    VARCHAR(100) NOT NULL,
    country  VARCHAR(100) NOT NULL DEFAULT 'India'
);

INSERT INTO pincodes (pincode, district, state) VALUES
    ('452001', 'Indore', 'Madhya Pradesh'),
    ('452010', 'Indore', 'Madhya Pradesh'),
    ('462001', 'Bhopal', 'Madhya Pradesh'),
    ('456001', 'Ujjain', 'Madhya Pradesh'),
    ('474001', 'Gwalior', 'Madhya Pradesh'),
    ('482001', 'Jabalpur', 'Madhya Pradesh'),
    ('302001', 'Jaipur', 'Rajasthan'),
    ('342001', 'Jodhpur', 'Rajasthan'),
    ('313001', 'Udaipur', 'Rajasthan'),
    ('400001', 'Mumbai', 'Maharashtra'),
    ('411001', 'Pune', 'Maharashtra'),
    ('440001', 'Nagpur', 'Maharashtra'),
    ('110001', 'New Delhi', 'Delhi'),
    ('226001', 'Lucknow', 'Uttar Pradesh'),
    ('208001', 'Kanpur', 'Uttar Pradesh'),
    ('282001', 'Agra', 'Uttar Pradesh'),
    ('221001', 'Varanasi', 'Uttar Pradesh'),
    ('380001', 'Ahmedabad', 'Gujarat'),
    ('395001', 'Surat', 'Gujarat'),
    ('122001', 'Gurugram', 'Haryana'),
    ('121001', 'Faridabad', 'Haryana'),
    ('141001', 'Ludhiana', 'Punjab'),
    ('143001', 'Amritsar', 'Punjab'),
    ('700001', 'Kolkata', 'West Bengal'),
    ('560001', 'Bengaluru', 'Karnataka'),
    ('570001', 'Mysuru', 'Karnataka');
