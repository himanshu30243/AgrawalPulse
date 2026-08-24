# AgrawalPulse — API Specifications (v1)

Base path: `/api/v1`. Auth: `Authorization: Bearer <JWT>` (Cognito-issued in cloud environments, local-dev-issued in `local` profile — identical claim shape: `sub`, `chapter_id`, `roles[]`). All endpoints are chapter-scoped from the JWT unless the caller holds a `NATIONAL_*` role.

## Auth
| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/auth/login` | public | **local profile only** — dev-mode credential exchange returning a locally-signed JWT. In cloud profiles, auth happens via Cognito Hosted UI / SDK, not this endpoint. |
| GET | `/auth/me` | any authenticated | Returns decoded principal (id, chapterId, roles) for frontend bootstrap. |

## Family
| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/families` | CHAPTER_ADMIN | Register family + members in one call. |
| GET | `/families` | CHAPTER_ADMIN | Paginated, chapter-scoped. |
| GET | `/families/{id}` | CHAPTER_ADMIN | |
| PUT | `/families/{id}` | CHAPTER_ADMIN | |
| POST | `/families/{id}/members` | CHAPTER_ADMIN | Add a family member (census entry). |
| PUT | `/families/{id}/members/{memberId}` | CHAPTER_ADMIN | |

## Membership
| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/memberships` | CHAPTER_ADMIN, TREASURER | Filter by `status`, `year`. |
| POST | `/memberships/{familyId}/renew` | TREASURER | Body: `{ amount, paymentMethod, transactionRef, year }`. Idempotent on `(familyId, year)`. |
| GET | `/memberships/{familyId}/payments` | CHAPTER_ADMIN, TREASURER | Payment history. |
| GET | `/memberships/collection-dashboard` | TREASURER | Chapter (or national, if role permits) collection summary. |

## Matrimony *(consent-gated — see Security Design)*
| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/matrimony/consent` | self or guardian (MEMBER) | `{ familyMemberId, scope: CHAPTER\|NATIONAL }`. Creates/renews consent. |
| DELETE | `/matrimony/consent/{familyMemberId}` | self or guardian, or CHAPTER_ADMIN | Revokes consent — immediate removal from search index. |
| GET | `/matrimony/eligible` | MATRIMONY_VIEWER | Query params: `district`, `education`, `profession`, `gender`, `chapterId?` (national role only). Returns only consented profiles. |
| GET | `/matrimony/eligible/{familyMemberId}` | MATRIMONY_VIEWER | Full consented profile detail. |

## Event
| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/events` | CHAPTER_ADMIN | |
| GET | `/events` | any authenticated | Chapter-scoped upcoming events. |
| POST | `/events/{id}/register` | MEMBER | Registers caller's family. |
| GET | `/events/{id}/registrations` | CHAPTER_ADMIN | |

## Analytics
*(as actually implemented in `analytics-service` — supersedes an earlier draft of this table that didn't match the built endpoints)*

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/analytics/families/total` | ADMIN, CHAPTER_ADMIN, TREASURER, NATIONAL_ADMIN | `?chapterId=` honored only for NATIONAL_ADMIN; omitted + national role = whole-federation total. |
| GET | `/analytics/memberships/active` | same | `?chapterId=&year=` (year defaults to current year). |
| GET | `/analytics/marriage-readiness/eligible-count` | same | Aggregate eligible-boys/eligible-girls/other counts only — never individual records; doesn't require `MATRIMONY_VIEWER` since it returns counts, not profiles. |
| GET | `/analytics/population/total` | same | `?chapterId=&gender=&city=`. `city` matches the chapter's own city (`chapters.city`), not `family.district`. No `chapterId` + no `city` + NATIONAL_ADMIN = nation-wide total; that's the "nation" view, not a separate endpoint. |
| GET | `/analytics/population/by-city` | same | `?chapterId=&gender=` — breakdown grouped by chapter city. |
| GET | `/analytics/population/by-state` | same | `?chapterId=&gender=` — breakdown grouped by chapter state. |

Not yet implemented: age/education/profession distribution endpoints (originally planned, not built).

## Conventions
- All list endpoints paginated: `?page=0&size=20`, response envelope `{ content, page, size, totalElements }`.
- Errors: RFC 7807 `application/problem+json` — `{ type, title, status, detail, instance }`.
- All timestamps ISO-8601 UTC.
- Write endpoints are idempotent where a natural key exists (membership renewal, event registration) — repeat calls return the existing resource (200) rather than erroring or duplicating.
