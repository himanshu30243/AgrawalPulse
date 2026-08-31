-- CHAPTER_ADMIN was seeded (V2) holding VIEW_ALL_FAMILIES alongside VIEW_FAMILY - a leftover from
-- before family-service had any scope tiers, when VIEW_ALL_FAMILIES was the only way to grant
-- "see more than just your own row" and every admin-ish role got it by default. V3 added the
-- correctly-scoped VIEW_CHAPTER_FAMILIES for this role but didn't remove the older, broader grant,
-- so a CHAPTER_ADMIN could still read every chapter nationwide - the opposite of "City/Chapter
-- Admin: cannot view families from other cities/chapters". Removing it here; NATIONAL_ADMIN and
-- ADMIN are untouched, VIEW_ALL_FAMILIES is correct for both of those.
DELETE FROM role_permissions
WHERE role_id = (SELECT role_id FROM roles WHERE role_code = 'CHAPTER_ADMIN')
  AND permission_id = (SELECT permission_id FROM permissions WHERE permission_code = 'VIEW_ALL_FAMILIES');
