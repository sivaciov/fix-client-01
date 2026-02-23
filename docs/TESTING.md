# Testing Guide

## Prerequisites

- Java 21
- Node.js 20+
- npm

## Ports Used by E2E

- Backend: `127.0.0.1:8080`
- Frontend: `127.0.0.1:5173`

Override with:
- `E2E_BACKEND_PORT`
- `E2E_FRONTEND_PORT`
- `E2E_HOST`
- `PLAYWRIGHT_BASE_URL` (defaults to frontend URL)

## Backend Unit Tests

```bash
cd backend
mvn -q test
```

## Frontend Unit Tests and Build

```bash
cd frontend
npm ci
npm test
npm run build
```

## Playwright E2E (Playwright-managed servers)

Use this when you want Playwright to start/stop backend + frontend itself:

```bash
cd frontend
npx playwright install --with-deps chromium
npm run test:e2e
```

## Local One-Command E2E Runner

Use this for deterministic startup and cleanup in local/dev/CI-like runs:

```bash
cd frontend
npx playwright install --with-deps chromium
npm run e2e:local
```

`npm run e2e:local` will:
1. Pick free backend/frontend ports (unless explicitly provided)
2. Start backend with `SERVER_PORT=<picked-port>` and wait for `/health`
3. Start frontend on the picked port with `VITE_BACKEND_PORT=<backend-port>` and wait for app readiness
4. Run Playwright headless
5. Stop both servers automatically

Environment overrides:
- `E2E_BACKEND_PORT`
- `E2E_FRONTEND_PORT`
- `PLAYWRIGHT_BASE_URL`
- `VITE_BACKEND_PORT` (mainly for manual `npm run dev`)
- `E2E_HOST` (default `127.0.0.1`)
- `BACKEND_START_TIMEOUT` (default `120`)
- `FRONTEND_START_TIMEOUT` (default `120`)

CI uses the same script via `npm run e2e:local:ci` with single-worker Playwright output.

## Full Local Validation Sequence

```bash
cd backend && mvn -q test
cd ../frontend && npm ci && npm test && npm run build
cd ../frontend && npx playwright install --with-deps chromium && npm run e2e:local
```

## Troubleshooting

- `Timed out waiting for Backend .../health`:
  - check backend logs in `${TMPDIR:-/tmp}/fix-client-backend-e2e-*.log`
  - confirm Java is installed
- `Timed out waiting for Frontend ...:5173`:
  - check frontend logs in `${TMPDIR:-/tmp}/fix-client-frontend-e2e-*.log`
- Browser install errors:
  - rerun `npx playwright install --with-deps chromium`
- Need slower startup tolerance:
  - run with `BACKEND_START_TIMEOUT=240 FRONTEND_START_TIMEOUT=240 npm run e2e:local`
