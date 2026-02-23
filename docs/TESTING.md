# Testing Guide

## Prerequisites

- Java 21 (Java 17 fallback is supported in CI)
- Node.js 20+
- npm

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

```bash
cd frontend
npx playwright install --with-deps chromium
npm run test:e2e
```

By default, Playwright starts:
- backend on `127.0.0.1:8080`
- frontend on `127.0.0.1:5173`

Optional environment overrides:
- `E2E_BACKEND_PORT`
- `E2E_FRONTEND_PORT`
- `PLAYWRIGHT_BASE_URL`

## Local E2E Runner (explicit server orchestration)

```bash
cd frontend
npx playwright install --with-deps chromium
npm run e2e:local
```

`npm run e2e:local` will:
1. Start backend and wait for `/health`
2. Start frontend and wait for the app URL
3. Run Playwright headless
4. Stop both servers automatically

## Full Local Validation Sequence

```bash
cd backend && mvn -q test
cd ../frontend && npm ci && npm test && npm run build
cd ../frontend && npx playwright install --with-deps chromium && npm run e2e:local
```
