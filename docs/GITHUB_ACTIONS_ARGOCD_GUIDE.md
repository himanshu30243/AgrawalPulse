# GitHub Actions + ArgoCD Guide

## What was added

This project already had a Jenkins pipeline (`Jenkinsfile`, `docs/JENKINS_SETUP.md`). This adds
a second, GitOps-flavored path alongside it - it doesn't touch or replace Jenkins. Pick whichever
fits the target environment; the two aren't meant to run against the same cluster at once.

```
Push to main
     │
     ▼
GitHub Actions (.github/workflows/ci-cd.yml)
  1. backend job   → ./mvnw clean verify              (full reactor, every module)
  2. frontend job  → npm ci && lint && test && build
  3. docker job    → build + push 10 images to GHCR, tagged :<git-sha> and :latest
  4. update-gitops-tag → bump helm/agrawalpulse/values.yaml's imageTag, commit to main
     │
     ▼
ArgoCD (watching helm/agrawalpulse/ in this same repo)
  - dev:  auto-sync, self-heal - rolls out within its poll interval (default 3 min) or
          instantly if you've wired the GitHub webhook (see below)
  - prod: shows OutOfSync, waits for `argocd app sync agrawalpulse-prod`
```

The CI workflow **never touches a cluster**. Its only job is: build, test, push images, bump one
line in Git. ArgoCD is the only thing that talks to Kubernetes - that split (CI builds artifacts,
CD is a controller reconciling desired state from Git) is the actual point of GitOps.

---

## Repo layout this added

```
.github/workflows/ci-cd.yml   CI/CD pipeline described above
helm/agrawalpulse/            One Helm chart, one release: every service + Redis + Ingress
  Chart.yaml
  values.yaml                 dev defaults - the source of truth for service topology
  values-prod.yaml            short diff: replicas, resource tiers, ingress host
  templates/
    deployment.yaml           ranges over values.yaml's `services:` list once
    service.yaml               "
    redis.yaml                 dedicated (different image/probes than the app services)
    ingress.yaml               frontend only - everything else is ClusterIP
    secret.yaml                PLACEHOLDER db credentials - see helm/agrawalpulse/README.md
    namespace.yaml
    _helpers.tpl
  README.md
argocd/
  project.yaml                 AppProject - scopes what these Applications can touch
  root-app.yaml                app-of-apps: the one thing you kubectl apply by hand
  application-dev.yaml
  application-prod.yaml
backend/mvnw, backend/mvnw.cmd, backend/.mvn/   Maven wrapper (was missing - see below)
backend/*/Dockerfile          rewritten - see below
backend/.dockerignore
frontend/nginx.conf.template  renamed from nginx.conf, fixed - see below
```

---

## Bugs this fixed along the way

These aren't new to GitHub Actions/ArgoCD - they were pre-existing, but only actually *execute*
once something builds real Docker images from this repo, which nothing had done yet.

1. **Missing Maven wrapper.** Every backend `Dockerfile` ran `./mvnw ...`, but `mvnw`/`.mvn/`
   didn't exist in the repo. The Docker base image (`eclipse-temurin:21-jdk-alpine`) has a JDK,
   not Maven - every image build would have failed on step one. Generated it via
   `mvn -N wrapper:wrapper -Dmaven=3.9.9`.

2. **Dockerfiles didn't know about the 3 newest modules.** The 6 existing service Dockerfiles
   individually `COPY`'d each module's `pom.xml` by name - a list that predated
   `eureka-server`, `config-server`, and `api-gateway` being added to the reactor. Maven's
   multi-module resolution needs every module's `pom.xml` physically present even when only
   building one of them (`-pl X -am`), so every one of those 6 Dockerfiles would have failed
   with "Child module ... does not exist." Rewrote all 9 backend Dockerfiles to `COPY . .`
   the whole `backend/` context in one step instead of hand-listing modules - the fragile
   pattern is exactly what caused this bug, so the fix removes the pattern, not just the
   symptom. A new `backend/.dockerignore` keeps that COPY from dragging in `target/` output.

3. **`frontend/nginx.conf` used bash syntax nginx can't parse.** The API proxy line was
   `proxy_pass http://${API_GATEWAY_HOST:-localhost:8080};` - nginx config has no notion of a
   `${VAR:-default}` fallback (that's shell/`docker-compose.yml` syntax), and nothing in the
   Dockerfile ran `envsubst` on it anyway, so the file was just copied to `conf.d/` verbatim
   and nginx would have tried to proxy to the literal, invalid hostname
   `${API_GATEWAY_HOST:-localhost:8080}`. Nothing had caught this because the frontend
   was never deployed as a real container in this project before now - `npm run dev`'s Vite
   proxy (`vite.config.ts`) never touches this file at all. Fixed by renaming it to
   `nginx.conf.template`, dropping the bash-only `:-default`, and pointing the Dockerfile at
   nginx's own official templating mechanism (`/etc/nginx/templates/*.template` +
   `docker-entrypoint.sh`'s built-in `envsubst` step), which only substitutes real environment
   variables and leaves nginx's own `$host`/`$remote_addr`/etc. alone.

---

## One-time cluster setup

You need a Kubernetes cluster with an `ingress-nginx` controller and ArgoCD installed. For a
local demo, `kind` or `minikube` both work.

```bash
# Install ArgoCD (skip if it's already on the cluster)
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Install an ingress controller (skip if already present)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# Bootstrap: this ONE apply creates the AppProject + both Applications for you
kubectl apply -f argocd/root-app.yaml -n argocd
```

Get the initial admin password and open the UI:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
kubectl -n argocd port-forward svc/argocd-server 8080:443
# https://localhost:8080  (user: admin)
```

You should see `agrawalpulse-root` → `agrawalpulse-dev` and `agrawalpulse-prod` appear within
its poll interval.

---

## GitHub Actions setup

Nothing to configure. `docker/login-action` authenticates to GHCR with the workflow's own
built-in `GITHUB_TOKEN` - no Docker Hub account, no PAT, no secrets to add. The only repo
setting to check:

**Settings → Actions → General → Workflow permissions → "Read and write permissions"**
(needed for the `update-gitops-tag` job to push the tag-bump commit back to `main`).

The first time an image is pushed, its GHCR package will be **private** by default and linked
to this repo - the cluster's `imagePullSecrets` would need a token with `read:packages` scope
to pull it. Either make the packages public (Settings → Packages → each package → Change
visibility) or create a pull secret:

```bash
kubectl create secret docker-registry ghcr-pull \
  --docker-server=ghcr.io \
  --docker-username=<your-github-username> \
  --docker-password=<a PAT with read:packages> \
  -n agrawalpulse-dev
```

then add `imagePullSecrets: [{name: ghcr-pull}]` under each Deployment's pod spec in
`helm/agrawalpulse/templates/deployment.yaml` if you go the private-package route.

---

## Faster dev sync (optional)

ArgoCD polls Git every 3 minutes by default. For instant sync instead of waiting, add a GitHub
webhook: **repo Settings → Webhooks → Add webhook**, payload URL
`https://<your-argocd-host>/api/webhook`, content type `application/json`, no secret needed
unless you've configured one in ArgoCD's own config, events: "Just the push event."

---

## Promoting dev → prod

```bash
argocd app sync agrawalpulse-prod
# or: click "Sync" on agrawalpulse-prod in the ArgoCD UI
```

Prod runs the same image tag dev is running (both overlays read the same
`global.imageTag` from `values.yaml`) - "promotion" here means *decide dev looks good*, not
*rebuild anything*. `values-prod.yaml` only changes replica counts, resource tiers, and the
ingress host.

---

## Local verification without a cluster

Docker and a real Kubernetes cluster weren't available in the environment this was built in, so
none of the above was executed end-to-end here - only the Maven build (`./mvnw -pl <module> -am
package`, confirmed working) and a manual line-by-line trace of every Helm template. Before
relying on this:

```bash
cd backend && ./mvnw -q clean verify              # full reactor build+test
helm lint helm/agrawalpulse                        # chart is well-formed
helm template helm/agrawalpulse -f helm/agrawalpulse/values.yaml | less   # eyeball rendered YAML
```

If Docker is available, sanity-check one image locally before trusting CI with all ten:

```bash
cd backend
docker build -f user-service/Dockerfile -t agrawalpulse/user-service:local .
docker run --rm -p 8081:8081 agrawalpulse/user-service:local
curl http://localhost:8081/actuator/health
```

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `docker` job fails: `Child module ... does not exist` | A new module was added to `backend/pom.xml` without a matching Dockerfile/context - shouldn't recur now that Dockerfiles `COPY . .` the whole reactor, but check `.dockerignore` didn't accidentally exclude a real source path |
| Image push succeeds, ArgoCD never updates | Check `update-gitops-tag` actually committed (Actions log) and that ArgoCD's `targetRevision: main` matches the branch that received the commit |
| ArgoCD shows `Unknown`/`ImagePullBackOff` | GHCR package is private and no `imagePullSecrets` configured - see GitHub Actions setup above |
| Pods `CrashLoopBackOff` on `user-service` etc. | Check `SPRING_DATASOURCE_URL` actually resolves - `global.database.host` in `values.yaml` is a deliberate non-resolving placeholder until you point it at a real Postgres |
| Frontend loads but API calls 502 | `API_GATEWAY_HOST` env var didn't get substituted - confirm the frontend container is reading `nginx.conf.template` from `/etc/nginx/templates/`, not a stale `/etc/nginx/conf.d/default.conf` baked into an older image layer |
