# AgrawalPulse — Deployment Architecture

## 1. Environments
| Environment | Where | Purpose |
|---|---|---|
| `local` | Developer laptop, Docker Compose | Day-to-day development, zero AWS dependency/cost |
| `dev` | AWS (`ap-south-1`) | Shared integration environment, deployed on every merge to `main` |
| `staging` | AWS (`ap-south-1`) | Pre-prod, production-like data volume for testing | 
| `prod` | AWS (`ap-south-1`) | Live | 

Same Docker image and Terraform module set is promoted across `dev`→`staging`→`prod`; only environment variables/parameter values differ (12-factor).

## 2. Local (Docker Compose)
**Superseded from a single Spring Boot app to 6 containerized microservices** — see `docs/microservices-contract.md`. `docker-compose.yml` at repo root brings up: `postgres`, `redis`, `dynamodb-local`, `opensearch`, `localstack` (S3/SNS/SES/EventBridge emulation), all six backend services (`user-service:8081`, `family-service:8082`, `membership-service:8083`, `matrimony-service:8084`, `event-service:8085`, `analytics-service:8086`, each built from its own `Dockerfile`), and an `api-gateway` nginx container on port 8080 that path-routes to the six services (`nginx/local-gateway.conf`) — a local stand-in for API Gateway, so the frontend's `VITE_API_BASE_URL=http://localhost:8080` never has to know six services exist. Frontend still runs separately via `npm run dev` (Vite dev server, not containerized). One command (`docker compose up -d`) gets a developer to a fully working backend with no AWS account. See `README.md` for exact steps.

## 3. AWS Target Architecture

```
Internet
   │
   ▼
Route 53 → CloudFront (static frontend, S3 origin)
   │
   ▼
API Gateway (REST, Cognito JWT authorizer, WAF attached, path-routes to 6 services)
   │
   ├──────┬──────┬──────┬──────┬──────┐
   ▼      ▼      ▼      ▼      ▼      ▼
 user   family member. matri- event analytics   ← 6 ECS Fargate services,
 -svc   -svc  -ship    mony   -svc  -svc            each own task def,
  ALB    ALB  -svc ALB -svc    ALB   ALB              target group, auto-scaling
              ALB      ALB
   │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼
        Aurora PostgreSQL (Multi-AZ, shared instance, service-owned tables)
              + DynamoDB (audit/session) + Redis (analytics-service cache)
              + OpenSearch (matrimony-service index)

S3 (documents, exports, Athena source) ── Glue (catalog/ETL) ── Athena (ad-hoc query) ── QuickSight (BI dashboards)

EventBridge (domain events) → SNS/SES (notifications)
```

- Frontend is a static build served from S3 via CloudFront (not run on ECS) — it's a static SPA, not a server-rendered app, so this is both cheaper and simpler than containerizing it.
- **Six ECS Fargate services**, each with its own task definition, ALB target group, and independent auto-scaling policy (target-tracking on CPU/request count) — this is the whole point of the split: `matrimony-service` (sensitive, latency-tolerant) scales independently from `analytics-service` (read-heavy, cacheable) or `membership-service` (bursty around renewal season). API Gateway routes by path prefix to the correct target group, same scheme as the local nginx gateway.
- Service-to-service calls (`membership-service`/`event-service`/`matrimony-service` → `family-service`) resolve via internal service discovery (AWS Cloud Map / ECS Service Connect) rather than the public API Gateway path, keeping inter-service traffic off the internet-facing edge.
- Aurora PostgreSQL: Multi-AZ in staging/prod, single instance in dev — one shared instance across all six services (confirmed design choice over per-service databases), each running its own Flyway migration history.
- Athena/Glue/QuickSight operate on exports/CDC from Aurora into S3 (via a scheduled Glue job) — kept out of the transactional request path entirely, per the CQRS boundary in the HLD.

## 4. CI/CD
1. PR merged to `main` → CI builds the Maven reactor (`common` + 6 services) → runs tests → builds **6 separate Docker images** (one per service, tagged independently so a change to `matrimony-service` doesn't force-redeploy the other five) + the frontend's static bundle → pushes images to ECR, bundle to S3 (dev). Path-based change detection (only rebuild/redeploy images for services whose code actually changed) is worth adding once the six-service build starts taking meaningfully longer than the old single build.
2. Deploy to `dev` automatic; `staging` and `prod` promotions are manual-approval gated.
3. Terraform plan runs on every infra PR; apply requires manual approval for `staging`/`prod`. Terraform now needs one ECS service + task definition + target group per microservice (6x the resources of the old single-service module) — not yet written.
4. Each service's Flyway migration runs as its own pre-deploy ECS task (not on app boot) against only its own tables, so schema changes are decoupled from app rollout and reviewable independently per service — six independent migration histories in the one shared database (`spring.flyway.table` distinct per service, see `microservices-contract.md`).

## 5. Observability
- CloudWatch Logs (structured JSON) from ECS tasks; CloudWatch Alarms on error rate, latency, DB connections.
- X-Ray tracing across API Gateway → ECS → Aurora/Redis/OpenSearch for request-level tracing.
- Health checks: ALB target group health check on `/actuator/health`; ECS service auto-replaces unhealthy tasks.

## 6. Disaster Recovery
- Aurora automated backups + PITR (RPO 15 min); cross-region snapshot copy to `ap-south-2` for DR once at national scale (not required at launch — see HLD §6/7).
- Terraform state itself is versioned and recoverable (S3 + DynamoDB lock table), so infrastructure can be rebuilt from code in a new region if needed.
