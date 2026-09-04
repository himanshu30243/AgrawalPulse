-- Fills a gap found in live use: 411018 (a common Pune PIN) wasn't in V4's seed set, so the
-- lookup 404'd and the wizard fell back to manual State/District selection. Same "curated
-- subset, extend as gaps are found" approach as V4 - adding a few more well-known Pune codes
-- rather than just the one reported, since the same gap will otherwise resurface immediately.
INSERT INTO pincodes (pincode, district, state) VALUES
    ('411002', 'Pune', 'Maharashtra'),
    ('411004', 'Pune', 'Maharashtra'),
    ('411005', 'Pune', 'Maharashtra'),
    ('411014', 'Pune', 'Maharashtra'),
    ('411018', 'Pune', 'Maharashtra'),
    ('411038', 'Pune', 'Maharashtra');
