# Agent Workflow Rules

This repository is worked on by multiple agents in parallel:
- 3 writer agents
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
- Required checks must pass.
