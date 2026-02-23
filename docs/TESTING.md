# Testing Guide

## Prerequisites

- Java 21
- Node.js 20+
- npm

## Ports Used by E2E

- Backend: `127.0.0.1:8080`
- Frontend: `127.0.0.1:5173`

Override with:
- `E2E_BACKEND_HOST`, `E2E_BACKEND_PORT`
- `E2E_FRONTEND_HOST`, `E2E_FRONTEND_PORT`
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

`npm run e2e:local` does all of the following:
1. Verifies required ports are free
2. Starts backend (`mvn -q -Dmaven.test.skip=true spring-boot:run`) and waits for `/health`
3. Starts frontend (`vite --strictPort`) and waits for app URL
4. Runs Playwright tests
5. Tears down both servers, even on failure (`trap` cleanup)

CI-optimized variant:

```bash
cd frontend
npm run e2e:local:ci
```

## Full Local Validation Sequence

```bash
cd backend && mvn -q test
cd ../frontend && npm ci && npm test && npm run build
cd ../frontend && npx playwright install --with-deps chromium && npm run e2e:local
```

## Troubleshooting

- `Timed out waiting for Backend .../health`:
  - check backend logs in `${TMPDIR:-/tmp}/fix-client-backend-e2e.log`
  - confirm Java is installed and `8080` is free
- `Timed out waiting for Frontend ...:5173`:
  - check frontend logs in `${TMPDIR:-/tmp}/fix-client-frontend-e2e.log`
  - confirm `5173` is free
- `Backend port 8080 is already in use` / `Frontend port 5173 is already in use`:
  - stop the conflicting process, or pick alternate ports with `E2E_BACKEND_PORT` / `E2E_FRONTEND_PORT`
- Browser install errors:
  - rerun `npx playwright install --with-deps chromium`
- Need slower startup tolerance:
  - run with `E2E_STARTUP_TIMEOUT_SECONDS=240 npm run e2e:local`
