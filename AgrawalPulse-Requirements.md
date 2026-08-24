# AgrawalPulse — Product & Architecture Requirements

## Business Goal
AgrawalPulse is a national, multi-chapter **Community Intelligence, Census, Membership Management, and Matrimonial Readiness Platform** for Agrawal Samaj, serving multiple regional chapters (cities/states) under one platform.

## Primary Objectives
1. Family Registration
2. Community Census
3. Membership Management
4. Marriage Readiness Identification *(renamed from "Eligibility" — see Terminology note)*
5. Analytics Dashboard
6. Event Management
7. AI-Ready Architecture

## Multi-Tenancy Model
- Platform serves many **Chapters** (one per city/region), each with its own admin hierarchy, membership collection, and event calendar.
- Data is chapter-scoped by default; national-level analytics aggregate across chapters for authorized national-level roles only.
- Tenant isolation enforced at the data layer (row-level chapter_id scoping) and at the API/auth layer (chapter claim in JWT).

## Technology Stack

**Frontend:** React, TypeScript, Material UI. Single responsive codebase — **must work on both mobile and laptop/desktop** (MUI breakpoints, mobile-first layout; no separate native app for v1). UI must support **Hindi and at least one regional language** alongside English (i18n from day one — the primary user base, especially older members submitting census data, is not English-only).

**Backend:** Java 21, Spring Boot 3, Spring Security, JWT.

**AWS Services:** Cognito, API Gateway, ECS Fargate, Aurora PostgreSQL, DynamoDB, ElastiCache Redis, OpenSearch, S3, SNS, SES, EventBridge, Athena, Glue, QuickSight.

**Architecture Patterns:** Microservices, Event-Driven Architecture, CQRS, API Gateway Pattern, Saga Pattern, Distributed Caching.

*(Full stack confirmed as appropriate given national multi-chapter scale — this is not a single-chapter deployment.)*

## Environment Strategy (new — local + AWS, kept in parity)
The application must run **locally first** (for development/testing) and **on AWS** (for staging/production), from the same codebase, switched by Spring profile (`local`, `dev`, `staging`, `prod`) and frontend `.env` files — no code forks between environments, only configuration.

**Local environment**: Docker Compose spins up substitutes for every managed AWS service so the full app runs on a laptop with no AWS account required:

| AWS Service (cloud) | Local substitute (Docker Compose) |
|---|---|
| Aurora PostgreSQL | `postgres` container |
| DynamoDB | `amazon/dynamodb-local` container |
| ElastiCache Redis | `redis` container |
| OpenSearch | `opensearchproject/opensearch` container |
| S3 | LocalStack or `minio` container |
| SNS / SES / EventBridge | LocalStack (or stubbed/no-op in local profile) |
| Cognito | Local dev-mode auth (simplified JWT issuer) — Cognito itself has no practical local emulator; `local` profile issues its own signed JWTs matching the same claim shape (`sub`, `chapter_id`, roles) so backend auth code doesn't branch by environment |
| API Gateway | Not used locally — React app calls the Spring Boot service(s) directly |
| ECS Fargate | Not used locally — services run via `docker-compose up` (or directly via Gradle/Maven) |
| Athena / Glue / QuickSight | Not applicable locally — these are AWS-side batch analytics/BI, exercised only in cloud environments |

**AWS environment**: full managed-service stack as listed above, deployed via **Terraform** (standard choice — mature modules for ECS Fargate/Aurora/DynamoDB/OpenSearch, cloud-agnostic, largest ecosystem), fronted by API Gateway + ECS Fargate, with staging and prod as separate AWS accounts or namespaced environments. Terraform state kept in a versioned, locked S3 backend with DynamoDB state locking.

**Parity rule**: business logic, entity models, and API contracts are identical across local and AWS; only infrastructure wiring (connection strings, auth provider, secrets source) changes per profile.

## Modules
1. User Module
2. Family Module
3. Membership Module
4. Matrimony Module *(consent-gated — see Data Privacy)*
5. Event Module
6. Analytics Module

## Membership Features
- Every family belongs to a Chapter, and every Chapter belongs to Agrawal Samaj nationally.
- Annual Membership Fee = ₹250 (per family; confirm if this varies by chapter or is fixed nationally).
- Membership renewal tracking, active/inactive status, collection dashboard, payment history — scoped per chapter with national roll-up.

## Marriage Readiness Features *(renamed from "Marriage Eligibility")*
> **Terminology note:** "Eligibility" implies a legal threshold. Indian legal marriageable age is 18 (women) / 21 (men) under current law. The ages below (21/24) are **community-preferred readiness ages**, not legal eligibility — renaming avoids implying anything is illegal below these ages, and avoids conflating with the pending national bill to raise women's legal age to 21.

- Girls: readiness threshold age ≥ 21
- Boys: readiness threshold age ≥ 24
- Automatic categorization based on family/census data
- District-wise, education-wise, and profession-wise dashboards

## Data Privacy & Compliance (new — was missing)
Marriage-readiness and matrimonial data (age, education, profession of unmarried individuals) is sensitive personal data under **India's Digital Personal Data Protection (DPDP) Act, 2023**. Requirements:
- **Consent-gated visibility**: matrimonial-category fields are hidden by default; each individual (or guardian, for census-only entries) must explicitly opt in before their record appears in matrimonial dashboards or is matchable.
- **Separate access tier**: matrimonial data access is a distinct permission from general census/membership admin access — a chapter treasurer collecting dues should not automatically see matrimonial profiles.
- **Right to erasure/correction**: individuals can request removal or correction of their data per DPDP requirements.
- **Consent audit trail**: who consented, when, and to what scope (chapter-visible vs national-visible) must be recorded.
- **AI matchmaking guardrail**: any future AI matchmaking recommendation must only operate on consented, opted-in profiles — never inferred from census data without explicit opt-in.

## Analytics
Total Families, Total Population, Active Members, Membership Collection, Age Distribution, Education Distribution, Profession Distribution, Eligible Boys Count, Eligible Girls Count — all available per-chapter and nationally aggregated, respecting matrimonial data consent scoping (i.e., readiness *counts* can be national aggregates even when individual profiles are chapter/consent-restricted).

## Non-Functional Requirements (new — was missing, needed before HLD can be finalized)
- **Expected scale (assumed)**: Launch — 20 chapters, ~500 families/chapter (~10,000 families, ~40,000 individuals). 3-year horizon — 150 chapters, ~1,500 families/chapter (~225,000 families, ~900,000 individuals). Architecture sized for the 3-year number, not launch-day traffic.
- Environments: local (Docker Compose) → dev / staging / prod on AWS, with CI/CD pipeline promoting the same build across them.
- Responsive UI: functional and usable on mobile (phone) and laptop/desktop viewports from a single build.
- Availability/SLA target: 99.5% for launch (single-region), RTO 4h / RPO 15min via Aurora automated backups + PITR.
- **Region (assumed)**: single-region `ap-south-1` (Mumbai) at launch — matches the primary user base in India. Aurora and S3 cross-region replication to `ap-south-2` (Hyderabad) planned as a DR posture once national scale is reached, not an active-active multi-region deployment at launch.

## Deliverables to Generate
1. Enterprise HLD
2. Detailed LLD
3. AWS Architecture Diagram
4. Database Schema (with chapter_id tenant scoping and consent tables)
5. API Specifications
6. Spring Boot Project Structure (modular monolith → microservices boundary map, with `local`/`dev`/`staging`/`prod` Spring profiles)
7. Security Design (incl. DPDP consent model, RBAC tiers, local vs Cognito auth)
8. Deployment Architecture (Docker Compose for local; Terraform-provisioned ECS Fargate stack for AWS)
9. Cost Optimization Strategy
10. AI Integration Roadmap
11. `docker-compose.yml` and local setup guide (README) for running the full stack on a laptop

## Future AI Scope
- AI Community Assistant
- AI Search
- AI Matchmaking Recommendations (consent-gated only — see Data Privacy)
- Population Prediction
- Membership Renewal Prediction
