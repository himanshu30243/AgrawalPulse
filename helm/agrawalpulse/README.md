# agrawalpulse Helm chart

One chart, one release, 9 backend services + the frontend + Redis. `templates/deployment.yaml`
and `templates/service.yaml` each range over `values.yaml`'s `services:` list once - adding a
service means adding one entry there, not a new pair of manifest files to keep in sync.

## Local sanity checks (do this before ArgoCD ever sees a change)

```bash
helm lint helm/agrawalpulse
helm template helm/agrawalpulse -f helm/agrawalpulse/values.yaml | less
helm template helm/agrawalpulse -f helm/agrawalpulse/values.yaml -f helm/agrawalpulse/values-prod.yaml | less
```

## Database credentials

`templates/secret.yaml` ships a **placeholder** Secret so the chart deploys end-to-end out of
the box. Before pointing this at anything real:

- **Local/dev cluster:** `helm upgrade --install agrawalpulse . --set-string global.secrets.dbPassword=<real password>`
- **Real dev/prod:** delete `templates/secret.yaml` and manage the Secret with a
  SealedSecret, the External Secrets Operator, or a manually-applied Secret that's excluded
  from Git entirely. ArgoCD should never be the thing writing real credentials into Git.

## Postgres itself

Not part of this chart on purpose - a primary datastore needs its own backup/HA story that
doesn't belong tied to an application release. Point `global.database.host` at your managed
Postgres (RDS, Cloud SQL, or a long-lived instance you run yourself).

## What IS deployed

| Kind | Why |
|---|---|
| eureka-server, config-server, api-gateway, 6 business services, frontend | the application |
| Redis | disposable cache - fine to co-deploy, unlike Postgres |
| Ingress (frontend only) | the only externally-reachable piece; everything else is ClusterIP |

See `docs/GITHUB_ACTIONS_ARGOCD_GUIDE.md` at the repo root for the full CI → GHCR → Git → ArgoCD flow.
