import { defineConfig, devices } from '@playwright/test'

const backendPort = Number(process.env.E2E_BACKEND_PORT ?? '8080')
const frontendPort = Number(process.env.E2E_FRONTEND_PORT ?? '5173')
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://127.0.0.1:${frontendPort}`
const useManagedWebServers = process.env.PLAYWRIGHT_DISABLE_WEBSERVER !== '1'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  webServer: useManagedWebServers
    ? [
        {
          command: `cd ../backend && mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=${backendPort}`,
          url: `http://127.0.0.1:${backendPort}/health`,
          reuseExistingServer: true,
          timeout: 120_000,
        },
        {
          command: `npm run dev -- --host 127.0.0.1 --port ${frontendPort}`,
          url: `http://127.0.0.1:${frontendPort}`,
          reuseExistingServer: true,
          timeout: 120_000,
        },
      ]
    : undefined,
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
