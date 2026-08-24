# AgrawalPulse — Cost Optimization Strategy

Note: this is a community-org platform funded by ₹250/year family dues — cost discipline is a real constraint, not a nice-to-have. Estimates below are directional, not quotes.

## 1. Design-Level Savings (already baked into the architecture)
- **Modular monolith, not literal microservices**: one ECS Fargate service instead of 6+ separate services means one set of ALB/logging/monitoring/idle-capacity overhead instead of many. This is the single biggest cost lever in this design.
- **Static frontend on S3+CloudFront**, not a second compute target — no server cost for the SPA at all.
- **CQRS applied narrowly** (materialized view, not a second live database) avoids running a duplicate write-side/read-side data store.
- **No Saga orchestrator service** — avoided until a real distributed-transaction need exists.

## 2. Environment-Tiering
| Environment | Aurora | ECS Fargate | Notes |
|---|---|---|---|
| `dev` | Single instance (no Multi-AZ), smallest instance class | 1 task, no auto-scaling | Torn down outside business hours via scheduled scaling to 0 is viable if the team is India-hours-only |
| `staging` | Single instance, right-sized to prod-like data volume only when actively testing | 1-2 tasks | |
| `prod` | Multi-AZ, reserved/Aurora I/O-Optimized once traffic is predictable | 2+ tasks, auto-scaled | |

## 3. Compute
- Fargate Spot for `dev`/`staging` tasks (up to ~70% cheaper), on-demand only for `prod`.
- Right-size task CPU/memory from actual CloudWatch utilization after the first month rather than guessing upfront.
- Savings Plans for `prod` Fargate once usage is stable and predictable (typically after 3-6 months of steady traffic).

## 4. Data Tier
- Aurora Serverless v2 is worth evaluating for `dev`/`staging` (scales to near-zero when idle) — for `prod`, provisioned is more predictable once traffic is steady.
- ElastiCache: smallest node type that meets latency needs; cluster mode only once cache dataset size actually requires sharding — not by default.
- OpenSearch: single-AZ, smallest instance for `dev`; only the matrimony-search dataset lives here (small, filtered subset) — don't let this become a general-purpose search cluster.
- DynamoDB on-demand billing mode (not provisioned) — audit-log/session-cache access patterns are spiky and low enough volume that on-demand is cheaper than provisioned capacity planning.

## 5. Storage & Analytics
- S3 lifecycle rules: move export/report data to S3 Infrequent Access after 30 days, Glacier after 1 year.
- Athena/Glue costs are pay-per-query/per-job — keep the Glue ETL job scheduled (e.g., nightly), not continuously running.
- QuickSight: use the per-session pricing model for report consumers (chapter admins checking dashboards occasionally) rather than per-user licensing, given usage will be infrequent per individual.

## 6. Networking
- Single NAT Gateway per environment (not per-AZ) for `dev`/`staging` — accept the small availability tradeoff there; `prod` gets one per AZ for resilience.
- VPC endpoints for S3/DynamoDB to avoid NAT Gateway data-transfer charges on that traffic.

## 7. Monitoring the Monitoring
- CloudWatch Logs retention: 30 days for `dev`/`staging`, longer only for `prod` where compliance/audit needs justify it (see Security Design's DPDP audit trail requirement — that data has its own retention policy in DynamoDB, not CloudWatch).
- Set AWS Budgets alerts per environment so a runaway resource is caught within a day, not a billing cycle.

## 8. Review Cadence
Revisit this document quarterly against actual CloudWatch Cost Explorer data — every number above is a launch-time estimate, not a fixed target.
