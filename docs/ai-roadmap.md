# AgrawalPulse — AI Integration Roadmap

All AI features here are additive to the v1 platform (docs/HLD.md), not required for launch. Ordered roughly by dependency and risk.

## Phase 0 — Foundations (part of v1, not "AI" yet but required for it)
- Clean, structured data (census, membership, matrimony) is the actual prerequisite for everything below — the schema in `database-schema.md` and the consent model in `security-design.md` are what make AI features possible without a redesign later.
- Event stream (EventBridge domain events) already emitted for core actions — future AI features can subscribe rather than needing new instrumentation.

## Phase 1 — AI Search (lowest risk, highest immediate value)
- Natural-language query over already-consented, already-public-facing data (e.g., admin asks "families in Indore with no active membership this year" instead of building a filter UI for every combination).
- Implementation: a thin service that translates natural language to parameterized queries against the existing `analytics` read models — **not** an LLM with direct DB write access, and never with access to ungated matrimony records.

## Phase 2 — AI Community Assistant
- Chat-style assistant for chapter admins: "how do I renew a family's membership," "what's our chapter's collection rate this year" — backed by the same analytics/API layer as the dashboards, plus a curated FAQ/help corpus.
- Guardrail: assistant answers from retrieved platform data and documentation only (RAG), not open-ended generation about individuals — especially matrimony-related queries, which must still go through the existing consent + `MATRIMONY_VIEWER` authorization, not be answerable by the assistant bypassing that check.

## Phase 3 — Predictive Analytics
- **Membership renewal prediction**: a model flagging families likely to lapse (based on payment history patterns already in `membership_payment`), surfaced to treasurers as a proactive outreach list — assistive, not automated cancellation/action.
- **Population prediction**: trend projection over census growth per chapter/district for capacity/event planning, using existing `chapter_analytics_mv` history (requires starting to snapshot the materialized view over time, not just its current state).

## Phase 4 — AI Matchmaking Recommendations (highest sensitivity — last, and only with explicit design review)
- Recommends potential matches **only** among profiles with **live, explicit matrimony consent** (same gate as the existing matrimony search — no separate, looser data path for AI).
- Must be explicitly opt-in as a *separate* consent flag from basic matrimony-search visibility ("visible in search" ≠ "included in AI-recommended matches") — a family may want the former without the latter.
- No recommendation may be generated or shown without going through the same `MATRIMONY_VIEWER`-equivalent authorization as manual search; recommendations are a ranking over the existing consented dataset, not a new data source.
- Requires a documented fairness/bias review before launch (education/profession/district signals can encode caste or economic bias if not deliberately checked) — this is a review gate, not a technical afterthought.

## Cross-Cutting AI Guardrails (apply to every phase above)
1. No AI feature gets a broader data-access scope than an equivalent human admin action already has.
2. Matrimony data specifically: AI features are additive consumers of the existing consent gate, never a bypass of it.
3. All AI-assisted actions (renewal-risk flags, match suggestions) are advisory to a human, not autonomous — no auto-renewal cancellation, no auto-sent match notifications without admin/user review.
