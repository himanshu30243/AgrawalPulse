# AgrawalPulse

Community Intelligence, Census, Membership Management and Matrimonial Readiness Platform for Agrawal Samaj.

## Documentation
Start with [`AgrawalPulse-Requirements.md`](AgrawalPulse-Requirements.md) for business context and scoping decisions, then:

| Doc | Covers |
|---|---|
| [`docs/HLD.md`](docs/HLD.md) | Enterprise high-level design, architectural style rationale |
| [`docs/LLD.md`](docs/LLD.md) | Module internals, package structure, key sequences |
| [`docs/database-schema.md`](docs/database-schema.md) | Aurora DDL, DynamoDB tables, Redis/OpenSearch usage |
| [`docs/api-specifications.md`](docs/api-specifications.md) | REST API v1 contract |
| [`docs/security-design.md`](docs/security-design.md) | Auth, tenant isolation, DPDP consent model |
| [`docs/deployment-architecture.md`](docs/deployment-architecture.md) | Local vs AWS environments, CI/CD |
| [`docs/cost-optimization.md`](docs/cost-optimization.md) | Cost levers by environment/service |
| [`docs/ai-roadmap.md`](docs/ai-roadmap.md) | Phased AI feature plan and guardrails |
| [`docs/aws-architecture-diagram.html`](docs/aws-architecture-diagram.html) | Visual architecture + local/AWS parity diagram |
| [`docs/microservices-contract.md`](docs/microservices-contract.md) | **Backend service split**: 6 services, ownership, ports, inter-service REST contract |

## Repository Layout
```
AgrawalPulse/
├── backend/            6 Spring Boot 3 / Java 21 microservices + a shared `common` library,
│                        one Maven reactor (see docs/microservices-contract.md)
├── frontend/            React + TypeScript + MUI (responsive: mobile + desktop)
├── docs/                 HLD, LLD, schema, API spec, security, deployment, cost, AI roadmap
├── nginx/                 local-gateway.conf — local stand-in for API Gateway (path-routes to the 6 services)
├── docker-compose.yml      Local infra + all 6 backend services + the nginx gateway
└── AgrawalPulse-Requirements.md
```

## Running Locally
No AWS account required — everything runs on Docker + your machine.

```powershell
# 1. Start local infra AND all 6 backend services + the local API-gateway (nginx) — one command
docker compose up -d

# 2. Frontend (Vite dev server — see frontend/README.md for details)
cd frontend
npm install
npm run dev
```

The frontend talks to one base URL (`http://localhost:8080`, the local nginx gateway) exactly as it
would talk to API Gateway in AWS — it never needs to know six services exist behind it. To iterate
on a single service from source instead of its container, see `backend/README.md`.

The `local` Spring profile substitutes every managed AWS service with its Docker Compose equivalent (see the parity table in `docs/deployment-architecture.md` and Figure 2 of the architecture diagram) — application code is identical to what runs on AWS; only configuration differs per Spring profile / frontend env file.

## Running on AWS
Deployed via Terraform to `ap-south-1`, promoted `dev` → `staging` → `prod`, one ECS Fargate service per microservice. See `docs/deployment-architecture.md` for the full topology and CI/CD flow. Terraform modules are not yet included in this repo — infra-as-code is the next deliverable after the application scaffolding.

## Status
Architecture and scaffolding phase — see `docs/` for design docs, `docs/microservices-contract.md` for the backend service split, and `backend/`, `frontend/` for the current code.
