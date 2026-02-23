#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

HOST="${E2E_HOST:-127.0.0.1}"
BACKEND_START_TIMEOUT="${BACKEND_START_TIMEOUT:-120}"
FRONTEND_START_TIMEOUT="${FRONTEND_START_TIMEOUT:-120}"
PLAYWRIGHT_WORKERS="${PLAYWRIGHT_WORKERS:-}"
PLAYWRIGHT_REPORTER="${PLAYWRIGHT_REPORTER:-}"

RUN_ID="${E2E_RUN_ID:-$RANDOM-$RANDOM-$$}"
BACKEND_LOG="${TMPDIR:-/tmp}/fix-client-backend-e2e-${RUN_ID}.log"
FRONTEND_LOG="${TMPDIR:-/tmp}/fix-client-frontend-e2e-${RUN_ID}.log"

backend_pid=""
frontend_pid=""

pick_free_port() {
  node -e "const net=require('net');const s=net.createServer();s.listen(0,'${HOST}',()=>{console.log(s.address().port);s.close();});"
}

is_port_available() {
  local port="$1"
  node -e "const net=require('net');const s=net.createServer();s.once('error',()=>process.exit(1));s.listen(${port},'${HOST}',()=>s.close(()=>process.exit(0)));" >/dev/null 2>&1
}

pick_distinct_free_port() {
  local other_port="${1:-}"
  local candidate=""
  while true; do
    candidate="$(pick_free_port)"
    if [[ -n "$other_port" && "$candidate" == "$other_port" ]]; then
      continue
    fi
    if is_port_available "$candidate"; then
      echo "$candidate"
      return 0
    fi
  done
}

wait_for_url() {
  local url="$1"
  local timeout_seconds="$2"
  local started_at
  started_at="$(date +%s)"
  while true; do
    if curl --silent --show-error --fail "$url" >/dev/null 2>&1; then
      return 0
    fi

    if (( "$(date +%s)" - started_at >= timeout_seconds )); then
      return 1
    fi
    sleep 1
  done
}

cleanup() {
  set +e

  if [[ -n "${frontend_pid}" ]] && kill -0 "${frontend_pid}" >/dev/null 2>&1; then
    kill "${frontend_pid}" >/dev/null 2>&1 || true
    wait "${frontend_pid}" >/dev/null 2>&1 || true
  fi

  if [[ -n "${backend_pid}" ]] && kill -0 "${backend_pid}" >/dev/null 2>&1; then
    kill "${backend_pid}" >/dev/null 2>&1 || true
    wait "${backend_pid}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT INT TERM

BACKEND_PORT="${E2E_BACKEND_PORT:-$(pick_free_port)}"
FRONTEND_PORT="${E2E_FRONTEND_PORT:-$(pick_distinct_free_port "$BACKEND_PORT")}"
if [[ "$BACKEND_PORT" == "$FRONTEND_PORT" ]]; then
  FRONTEND_PORT="$(pick_distinct_free_port "$BACKEND_PORT")"
fi
BASE_URL="${PLAYWRIGHT_BASE_URL:-http://${HOST}:${FRONTEND_PORT}}"

echo "Backend log: ${BACKEND_LOG}"
echo "Frontend log: ${FRONTEND_LOG}"
echo "Starting backend on ${HOST}:${BACKEND_PORT}"

(
  cd "$BACKEND_DIR"
  SERVER_PORT="$BACKEND_PORT" mvn -q spring-boot:run
) >"$BACKEND_LOG" 2>&1 &
backend_pid="$!"

if ! kill -0 "${backend_pid}" >/dev/null 2>&1; then
  echo "Backend process exited before health checks. See ${BACKEND_LOG}" >&2
  exit 1
fi

if ! wait_for_url "http://${HOST}:${BACKEND_PORT}/health" "$BACKEND_START_TIMEOUT"; then
  echo "Backend failed to become ready. See ${BACKEND_LOG}" >&2
  exit 1
fi

echo "Starting frontend on ${HOST}:${FRONTEND_PORT}"
(
  cd "$FRONTEND_DIR"
  VITE_BACKEND_PORT="$BACKEND_PORT" npm run dev -- --host "$HOST" --port "$FRONTEND_PORT"
) >"$FRONTEND_LOG" 2>&1 &
frontend_pid="$!"

if ! kill -0 "${frontend_pid}" >/dev/null 2>&1; then
  echo "Frontend process exited before readiness checks. See ${FRONTEND_LOG}" >&2
  exit 1
fi

if ! wait_for_url "$BASE_URL" "$FRONTEND_START_TIMEOUT"; then
  echo "Frontend failed to become ready. See ${FRONTEND_LOG}" >&2
  exit 1
fi

echo "Running Playwright against ${BASE_URL}"

PLAYWRIGHT_CMD=(npx playwright test)
if [[ -n "$PLAYWRIGHT_WORKERS" ]]; then
  PLAYWRIGHT_CMD+=("--workers=$PLAYWRIGHT_WORKERS")
fi
if [[ -n "$PLAYWRIGHT_REPORTER" ]]; then
  PLAYWRIGHT_CMD+=("--reporter=$PLAYWRIGHT_REPORTER")
fi
PLAYWRIGHT_CMD+=("$@")

(
  cd "$FRONTEND_DIR"
  PLAYWRIGHT_DISABLE_WEBSERVER=1 \
    E2E_BACKEND_PORT="$BACKEND_PORT" \
    E2E_FRONTEND_PORT="$FRONTEND_PORT" \
    PLAYWRIGHT_BASE_URL="$BASE_URL" \
    "${PLAYWRIGHT_CMD[@]}"
)
