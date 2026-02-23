#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"

BACKEND_HOST="${E2E_BACKEND_HOST:-127.0.0.1}"
FRONTEND_HOST="${E2E_FRONTEND_HOST:-127.0.0.1}"
BACKEND_PORT="${E2E_BACKEND_PORT:-8080}"
FRONTEND_PORT="${E2E_FRONTEND_PORT:-5173}"
STARTUP_TIMEOUT_SECONDS="${E2E_STARTUP_TIMEOUT_SECONDS:-180}"
PLAYWRIGHT_WORKERS="${PLAYWRIGHT_WORKERS:-1}"
PLAYWRIGHT_REPORTER="${PLAYWRIGHT_REPORTER:-line}"

BACKEND_URL="http://${BACKEND_HOST}:${BACKEND_PORT}/health"
FRONTEND_URL="http://${FRONTEND_HOST}:${FRONTEND_PORT}"
BASE_URL="${PLAYWRIGHT_BASE_URL:-$FRONTEND_URL}"

BACKEND_LOG="${TMPDIR:-/tmp}/fix-client-backend-e2e.log"
FRONTEND_LOG="${TMPDIR:-/tmp}/fix-client-frontend-e2e.log"

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  set +e
  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    kill "$FRONTEND_PID" 2>/dev/null || true
    wait "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

wait_for_url() {
  local url="$1"
  local timeout="$2"
  local label="$3"
  local pid="$4"
  local log_file="$5"
  local start

  start="$(date +%s)"

  while true; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$label process exited before becoming ready. See $log_file" >&2
      tail -n 80 "$log_file" >&2 || true
      return 1
    fi

    if curl --fail --silent --show-error --max-time 2 "$url" > /dev/null; then
      echo "$label is ready at $url"
      return 0
    fi

    if (( "$(date +%s)" - start >= timeout )); then
      echo "Timed out waiting for $label at $url after ${timeout}s" >&2
      return 1
    fi

    sleep 1
  done
}

require_port_free() {
  local port="$1"
  local label="$2"

  if lsof -nP -iTCP:"$port" -sTCP:LISTEN > /dev/null 2>&1; then
    echo "$label port $port is already in use. Free it or set a different E2E_*_PORT." >&2
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >&2 || true
    return 1
  fi
}

require_port_free "$BACKEND_PORT" "Backend"
require_port_free "$FRONTEND_PORT" "Frontend"

echo "Starting backend on ${BACKEND_HOST}:${BACKEND_PORT}"
(
  cd "$BACKEND_DIR"
  mvn -q -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT}"
) > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

wait_for_url "$BACKEND_URL" "$STARTUP_TIMEOUT_SECONDS" "Backend" "$BACKEND_PID" "$BACKEND_LOG"

echo "Starting frontend on ${FRONTEND_HOST}:${FRONTEND_PORT}"
(
  cd "$FRONTEND_DIR"
  npm run dev -- --host "$FRONTEND_HOST" --port "$FRONTEND_PORT" --strictPort
) > "$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!

wait_for_url "$FRONTEND_URL" "$STARTUP_TIMEOUT_SECONDS" "Frontend" "$FRONTEND_PID" "$FRONTEND_LOG"

echo "Running Playwright against ${BASE_URL}"
cd "$FRONTEND_DIR"
PLAYWRIGHT_DISABLE_WEBSERVER=1 PLAYWRIGHT_BASE_URL="$BASE_URL" npx playwright test --workers="$PLAYWRIGHT_WORKERS" --reporter="$PLAYWRIGHT_REPORTER" "$@"
