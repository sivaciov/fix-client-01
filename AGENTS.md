# Agentic Workflow (ALWAYS Followed)

This repository is worked on by multiple agents in parallel:
- 3 writer agents
- 1 tester/QA agent

## Roles
- Writer1, Writer2, Writer3: implement assigned fixes/features.
- Tester: owns correctness end-to-end, runs local + CI validation, and is allowed to patch code directly when needed to make the pipeline green.

## Hard Rules
- Never push to `main` directly.
- Every change goes through a Pull Request.
- Keep PRs small and single-purpose.
- Required checks must pass.

## Branch Naming
- Writers: `agent/writer1/<topic>`, `agent/writer2/<topic>`, `agent/writer3/<topic>`
- Tester: `agent/tester/<topic>`

## Definition of Done
1. Backend unit tests pass:
   - `cd backend && mvn -q test`
2. Frontend unit tests and build pass:
   - `cd frontend && npm ci && npm test && npm run build`
3. Playwright E2E passes locally (see Playwright section below).
4. GitHub Actions is green on the PR (Tester checks and fixes if red).

## Failure Loop (Mandatory)
- Tester runs the full validation suite.
- If anything fails locally or in CI, Tester assigns a concrete fix to one writer.
- Tester pulls the writer’s fix, re-runs the suite, and repeats until everything is green.

## Playwright E2E
- Install browsers/deps:
  - `cd frontend && npx playwright install --with-deps`
- Run tests:
  - `cd frontend && npx playwright test`
- Base URL is configurable via environment variable, with default `http://localhost:5173`.

## Local E2E Run
- Start backend on port `8080`.
- Start frontend on port `5173`.
- Then run Playwright tests from `frontend`.
