# AgrawalPulse — Frontend

React + TypeScript + Material UI single-page app for the AgrawalPulse community
platform (Agrawal Samaj). One responsive codebase for mobile and desktop, built
with [Vite](https://vitejs.dev/).

See `../AgrawalPulse-Requirements.md` for the full product/architecture context.

## Prerequisites

- Node.js 20 LTS or later
- npm 10 or later
- The backend API running somewhere reachable (local Docker Compose stack, or
  a deployed `dev`/`staging`/`prod` environment) — see the root repo for the
  Spring Boot service and `docker-compose.yml`.

## Getting started

```bash
cd frontend
npm install
cp .env.local.example .env.local   # edit if your local backend runs elsewhere
npm run dev
```

The dev server starts at `http://localhost:5173` and proxies API calls to
whatever `VITE_API_BASE_URL` is set to in `.env.local` (defaults to
`http://localhost:8080/api/v1`, matching the backend's `local` Spring profile
running via `docker-compose up`).

## How environment selection works

The app never hardcodes a backend URL or forks logic by environment — it
reads one variable, `VITE_API_BASE_URL`, via `import.meta.env` (see
`src/vite-env.d.ts` for the typed shape and `src/api/axiosClient.ts` for where
it's consumed). Three example files show the value per target:

| File | Used for | Backend target |
|---|---|---|
| `.env.local.example` | `npm run dev` on a laptop | `http://localhost:8080/api/v1` (Docker Compose backend) |
| `.env.development.example` | AWS `dev` environment build | placeholder dev API Gateway URL |
| `.env.production.example` | AWS `prod` environment build | placeholder prod API Gateway URL |

Copy the one you need to a real file (`.env.local`, `.env.development`, or
`.env.production` — all git-ignored) and fill in the real URL. Vite picks the
right file automatically based on the mode:

```bash
npm run dev                        # loads .env.local (mode "development")
npm run build                      # loads .env.production by default
npm run build -- --mode development  # loads .env.development instead
```

In CI/CD, the same build is promoted across `dev` → `staging` → `prod` per the
requirements doc's environment strategy — only the env file (or injected build
variables) changes, never the code.

## Authentication

The backend's `local` profile issues JWTs with the same claim shape
(`sub`, `chapter_id`, `roles`, …) that Cognito would produce in `dev`/`staging`/
`prod`, so this frontend's auth flow (`src/auth/`) is identical across every
environment: log in, receive a JWT, store it, attach it as a `Bearer` token on
every API call (`src/api/axiosClient.ts`), and decode its `roles` claim to
drive role-aware navigation (`src/layout/navConfig.ts`).

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Start the Vite dev server with HMR |
| `npm run build` | Type-check (`tsc -b`) and build a production bundle to `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

## Project structure

```
src/
  api/          Axios instance + one client module per backend resource
  auth/         AuthContext, JWT decode/claims, token storage
  components/   Shared UI pieces (loading/error state, stat tiles, charts)
  constants/    Static reference data (chapter list — see note in source)
  hooks/        Shared hooks (useAsync fetch-state hook)
  i18n/         react-i18next setup
  layout/       AppBar, responsive Drawer, nav config, language switcher
  locales/      en/ and hi/ translation.json
  pages/        One folder per module: families, membership, matrimony, events
  routes/       React Router routes + role-aware ProtectedRoute
  theme.ts      MUI theme + chart color tokens
  types/        Shared domain TypeScript types
```

## Internationalization

English and Hindi are wired up via `react-i18next`
(`src/locales/en/translation.json`, `src/locales/hi/translation.json`), with a
language switcher in the top app bar. The selected language persists in
`localStorage`. Adding a third language means adding a new
`src/locales/<code>/translation.json` file and registering it in
`src/i18n/i18n.ts`'s `SUPPORTED_LANGUAGES` and `resources`.

## Roles and navigation

Roles are decoded from the JWT (`src/auth/jwt.ts`) and drive which nav items
render (`src/layout/navConfig.ts`) and which routes are reachable
(`src/routes/AppRoutes.tsx` wraps role-restricted pages in `ProtectedRoute`).
Matrimony is split into two independently-gated routes, mirroring
`docs/api-specifications.md`: `/matrimony/consent` (`MyConsentPage.tsx`) is
reachable by any authenticated user — DPDP consent is a "self or guardian"
action, not an admin one — while `/matrimony/directory`
(`MatrimonyDirectoryPage.tsx`, the consented-profiles search/eligible list)
requires the `MATRIMONY_VIEWER` role. See the comments in `navConfig.ts` and
`AppRoutes.tsx`.
