import { expect, test } from '@playwright/test'

const uiTimeoutMs = 15_000
const apiTimeoutMs = 15_000

test('Health and FIX controls flow works', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Health Check' })).toBeVisible({
    timeout: uiTimeoutMs,
  })
  await expect(page.getByText('Backend: ok')).toBeVisible({ timeout: uiTimeoutMs })

  const fixSection = page.getByRole('region', { name: 'FIX controls' })
  await expect(fixSection).toBeVisible({ timeout: uiTimeoutMs })
  await expect(fixSection.getByRole('heading', { name: 'FIX Session' })).toBeVisible({
    timeout: uiTimeoutMs,
  })

  const startButton = fixSection.getByRole('button', { name: 'Start' })
  const stopButton = fixSection.getByRole('button', { name: 'Stop' })
  await expect(startButton).toBeVisible({ timeout: uiTimeoutMs })
  await expect(stopButton).toBeVisible({ timeout: uiTimeoutMs })
  await expect(fixSection.getByText(/Last event:/)).toBeVisible({ timeout: uiTimeoutMs })
  await expect(fixSection.getByText(/Last error:/)).toBeVisible({ timeout: uiTimeoutMs })
  await expect(fixSection.getByText(/Last updated:/)).toBeVisible({
    timeout: uiTimeoutMs,
  })

  const startResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/fix/start') &&
      response.request().method() === 'POST',
    { timeout: apiTimeoutMs },
  )
  await startButton.click()
  const startResponse = await startResponsePromise
  expect(startResponse.ok()).toBeTruthy()

  const stopResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/fix/stop') && response.request().method() === 'POST',
    { timeout: apiTimeoutMs },
  )
  await stopButton.click()
  const stopResponse = await stopResponsePromise
  expect(stopResponse.ok()).toBeTruthy()

  await expect(fixSection.getByText(/Status:/)).toBeVisible({ timeout: uiTimeoutMs })
})
