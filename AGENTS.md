# Agent Workflow Rules

This repo is worked on by multiple agents in parallel:
- 3 code writers
- 1 tester/QA agent

## Hard rules
- Never push to `main` directly.
- Always work on a branch named:
  - `agent/writer1/<topic>`
  - `agent/writer2/<topic>`
  - `agent/writer3/<topic>`
  - `agent/tester/<topic>`
- Every change goes through a Pull Request.
- Keep PRs small and single-purpose.
- All PRs must pass CI.

## Expectations
- Writers: implement features + add/adjust unit tests for new behavior.
- Tester: expands tests, adds integration tests, improves CI stability, fixes flaky tests.

## Commit message prefixes
- `backend: ...`
- `frontend: ...`
- `ci: ...`
- `docs: ...`
- `chore: ...`

## PR template
Include:
- What changed
- How to test
- Notes / follow-ups
