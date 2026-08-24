# AgrawalPulse — Security Design

## 1. Identity & Authentication
- **Cloud (dev/staging/prod)**: AWS Cognito User Pool is the identity provider. Frontend uses Cognito Hosted UI / SDK; backend validates JWTs against the pool's JWKS endpoint (Spring Security OAuth2 Resource Server).
- **Local**: Cognito has no faithful local emulator, so the `local` Spring profile enables a dev-only token issuer producing JWTs with the identical claim shape (`sub`, `chapter_id`, `roles`). This keeps every downstream authorization check environment-agnostic. The issuer is compiled out of / disabled in every non-local profile — enforced by Spring profile activation, not a runtime flag, so it cannot be accidentally left on in production.
- MFA enforced on Cognito for CHAPTER_ADMIN and NATIONAL_ADMIN roles at minimum.

## 2. Authorization Model
Roles: `MEMBER`, `TREASURER`, `CHAPTER_ADMIN`, `NATIONAL_ADMIN`, plus the cross-cutting `MATRIMONY_VIEWER` authority which is granted independently of the above — a CHAPTER_ADMIN does not automatically get matrimony access.

| Concern | Enforcement point |
|---|---|
| Chapter tenant isolation | `TenantContext` populated from JWT `chapter_id`; repositories chapter-scope by default; cross-chapter reads require `NATIONAL_*` and go through analytics aggregates only |
| Matrimony data access | Separate `MATRIMONY_VIEWER` authority, checked via `@PreAuthorize`, independent of chapter-admin rights |
| Write-path tenant spoofing | `chapter_id` is never accepted from client request bodies for writes — always derived server-side from the JWT |

## 3. Data Protection (DPDP Act 2023 alignment)
Matrimony/marriage-readiness data is the most sensitive class of personal data in this system. Controls:
1. **Consent-gated by default**: no `family_member` row is visible in matrimony search/dashboards without a live (non-revoked) `matrimony_consent` row. This is enforced at the query layer (see LLD §3.3), not just hidden in the UI.
2. **Revocation is immediate and destructive to derived data**: revoking consent removes the OpenSearch document synchronously in the same transaction path as the Postgres update.
3. **Audit trail**: every read of matrimony profile detail and every consent change is written to the DynamoDB `audit_log` table (chapterId, actorId, action, timestamp) — required for DPDP accountability/breach-investigation obligations.
4. **Minimal indexing**: the OpenSearch matrimony index carries only search/filter fields (age, gender, district, education, profession) — never name, DOB, address, or contact info (see database-schema.md §4).
5. **Right to erasure/correction**: `DELETE /matrimony/consent/{id}` plus a family-level data export/delete admin workflow (manual in v1, API-driven in a later iteration) to satisfy DPDP data-principal rights requests.
6. **Encryption**: TLS in transit everywhere (API Gateway, ALB→ECS, RDS connections); Aurora and S3 encrypted at rest with KMS-managed keys; DynamoDB encryption at rest (default).

## 4. Network & Infrastructure
- API Gateway is the sole public entry point; ECS Fargate tasks run in private subnets, no public IPs.
- Aurora, ElastiCache, and OpenSearch are in private subnets, security-group-restricted to the ECS task security group only.
- Secrets (DB credentials, JWT signing material for the local issuer, third-party API keys) in AWS Secrets Manager, injected into ECS tasks at runtime — never baked into images or committed to source.
- WAF on API Gateway for standard OWASP protections (rate limiting, SQLi/XSS pattern blocking) in cloud environments.

## 5. Application-Level Controls
- Input validation at the controller boundary (Bean Validation) — reject before any service logic runs.
- Parameterized queries only (JPA/Spring Data) — no string-concatenated SQL.
- CORS restricted to the known frontend origin(s) per environment.
- Rate limiting (Redis-backed) on public/unauthenticated endpoints (e.g., consent opt-in confirmation links sent by email).

## 6. What's Deliberately Out of Scope for v1
- Field-level encryption within Postgres (relying on at-rest encryption + access control instead) — revisit if a formal DPIA calls for it.
- Automated DPDP data-subject-request API (v1 handles this via admin workflow, not self-service API).
